# locustfile.py
"""
은행 SaaS Stress Test
──────────────────────────────────────────────────
• 신규 사용자 → 회원가입 → JWT 발급 → 3개 계좌 개설
• 전역 계좌 풀(global_account_pool)에 본인 계좌 등록
• 이후 반복 작업
  1. 입금(post /accounts/{id}/deposit)
  2. 출금(post /accounts/{id}/withdraw)
  3. 이체(post /transfers/)
     - 50%: 자신의 다른 계좌
     - 50%: 전역 풀에서 무작위 계좌
  4. 이체 결과 조회(get /transfers/{id})
  5. 잔액 조회(get /accounts/{id})
• 모든 요청에서 5XX 응답이 나오면 테스트 실패 처리
"""

import random
import uuid
from threading import Lock

from locust import HttpUser, task, between, events, constant_pacing

# ──────────────────────────────
# 전역 설정값 (필요시 한 곳만 수정)
# ──────────────────────────────
INIT_DEPOSIT_MIN = 500_000      # 최초 입금액 하한
INIT_DEPOSIT_MAX = 2_000_000      # 최초 입금액 상한
TX_AMOUNT_MIN    = 1           # 이후 거래 최소 금액
TX_AMOUNT_MAX    = 2_000_000       # 이후 거래 최대 금액
TRANSFER_LARGE   = 1_000_000   # 100만↑은 pending 처리
WAIT_TIME_SEC    = 5           # constant_pacing 간격
PASSWORD_TPL     = "P@ssw0rd!"
PHONE_TPL        = "+82-10-{:04d}-{:04d}"

# 전역 계좌 목록(모든 User 간 공유) & Lock
global_account_pool = []
global_pool_lock    = Lock()

global_user_counter = 1
global_user_counter_lock = Lock()

# ──────────────────────────────
# Helper 함수
# ──────────────────────────────
def random_amount(low=TX_AMOUNT_MIN, high=TX_AMOUNT_MAX):
    """지정 범위 내 임의 금액 반환"""
    return random.randint(low, high)

def assert_not_5xx(response):
    """5XX 발생 시 Locust 실패 처리"""
    if response.status_code >= 500:
        response.failure(f"Server Error: {response.status_code}")
    else:
        response.success()

# ──────────────────────────────
# Locust 사용자 정의
# ──────────────────────────────
class BankingUser(HttpUser):
    wait_time = constant_pacing(WAIT_TIME_SEC)  # 초당 1회 호출 비슷한 효과

    # ────────────────
    # 초기화 로직
    # ────────────────
    def on_start(self):
        """
        1) 회원가입 → 2) 토큰 발급 → 3) 3개 계좌 개설(+초기 입금)
        → 전역 계좌 풀에 등록
        """
        user_num = None
        global global_user_counter
        with global_user_counter_lock:
            global_user_counter += 1
            user_num = global_user_counter

        self.username = f"username{user_num:05d}"
        self.password = PASSWORD_TPL
        self._register_member()
        self._issue_token()
        self._open_three_accounts()

    # ----- 회원가입 -----
    def _register_member(self):
        payload = {
            "id":   self.username,
            "name": self.username,  # 테스트이므로 동일 이름
            "password": self.password,
            "phone": PHONE_TPL.format(random.randint(0, 9999),
                                      random.randint(0, 9999)),
        }
        with self.client.post("/members", json=payload, catch_response=True) as res:
            assert_not_5xx(res)

    # ----- JWT 발급 -----
    def _issue_token(self):
        payload = {"id": self.username, "password": self.password}
        with self.client.post("/members/tokens", json=payload,
                              catch_response=True) as res:
            assert_not_5xx(res)
            self.token = res.json()["token"]
            self.headers = {"Authorization": f"Bearer {self.token}"}

    # ----- 계좌 3개 개설 & 전역 풀 등록 -----
    def _open_three_accounts(self):
        self.my_accounts = []
        for _ in range(3):
            with self.client.post("/accounts/", headers=self.headers,
                                  catch_response=True) as res:
                assert_not_5xx(res)
                acc_id = res.json()["accountId"]
                self.my_accounts.append(acc_id)

            # 첫 개설 직후 약간의 초기 자금 입금
            amount = random_amount(INIT_DEPOSIT_MIN, INIT_DEPOSIT_MAX)
            self._deposit(acc_id, amount)

        # 전역 계좌 목록에 추가 (Lock 필수)
        with global_pool_lock:
            global_account_pool.extend(self.my_accounts)

    # ────────────────
    # 반복 Task 목록
    # ────────────────
    @task(4)
    def deposit_task(self):
        account_id = random.choice(self.my_accounts)
        amount     = random_amount()
        self._deposit(account_id, amount)

    @task(4)
    def withdraw_task(self):
        account_id = random.choice(self.my_accounts)
        amount     = random_amount()
        self._withdraw(account_id, amount)

    @task(6)
    def transfer_task(self):
        from_acc = random.choice(self.my_accounts)
        # 50%: 내 계좌 / 50%: 전역 풀
        if random.random() < 0.5 and len(self.my_accounts) > 1:
            to_acc = random.choice([a for a in self.my_accounts if a != from_acc])
        else:
            # 전역 풀 비어있을 가능성(초기)에 대비
            with global_pool_lock:
                to_acc = random.choice(global_account_pool) if global_account_pool else from_acc

        amount = random_amount()
        self._transfer(from_acc, to_acc, amount)

    @task(2)
    def balance_task(self):
        account_id = random.choice(self.my_accounts)
        self._get_balance(account_id)

    # ────────────────
    # 실제 API 호출 Wrappers
    # ────────────────
    def _deposit(self, account_id, amount):
        payload = {"amount": amount}
        with self.client.post(f"/accounts/{account_id}/deposit",
                              headers=self.headers, json=payload,
                              catch_response=True) as res:
            assert_not_5xx(res)

    def _withdraw(self, account_id, amount):
        payload = {"amount": amount}
        with self.client.post(f"/accounts/{account_id}/withdraw",
                              headers=self.headers, json=payload,
                              catch_response=True) as res:
            assert_not_5xx(res)

    def _transfer(self, from_acc, to_acc, amount):
        payload = {"from": from_acc, "to": to_acc, "amount": amount}
        with self.client.post("/transfers/", headers=self.headers, json=payload,
                              catch_response=True) as res:
            assert_not_5xx(res)
            if res.status_code in (200, 202):
                event_id = res.json()["eventId"]
                # 이체 후 즉시 상태 조회
                self._check_transfer(event_id)

    def _check_transfer(self, event_id):
        with self.client.get(f"/transfers/{event_id}",
                             headers=self.headers, catch_response=True) as res:
            assert_not_5xx(res)

    def _get_balance(self, account_id):
        with self.client.get(f"/accounts/{account_id}",
                             headers=self.headers, catch_response=True) as res:
            assert_not_5xx(res)

# ──────────────────────────────
# 테스트 통계 후킹: 5XX가 하나라도 있으면 빨간불 표시
# ──────────────────────────────
@events.request.add_listener
def on_request(request_type, name, response_time, response_length,
               response, context, exception, start_time, url, **kw):
    if response and response.status_code >= 500:
        # Locust 내부 집계 외에도 콘솔에 즉시 표시
        print(f"[ERROR] 5XX detected → {url} ({response.status_code})")
