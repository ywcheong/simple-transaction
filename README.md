# SimpleTransaction

이 프로젝트는 간단한 Toy 프로젝트이며, REST API를 통해 간단한 기능을 가진 데모 금융 시스템을 지원한다.

Jira 페이지는 다음과 같다. 

## 1. 프로젝트의 소개

### 1.1. 목적

이 프로젝트를 통해 다음과 같은 새로운 기반원리와 기술을 학습한다.

- 보안
	- Spring Security
	- JWT
- 데이터베이스
	- MySQL
	- DOMA

### 1.3. 의사결정 기준

이 프로젝트를 통해 유지보수가 용이한 코드를 작성하고, 단위 테스트를 관리하며, 관측가능성을 복습하고
기존 [SimpleWebsocketMQ](https://github.com/ywcheong/simple-websocket-mq)에서 배운 점을 토대로 코드베이스를 개선한다.

임의의 시점에 장애의 발생을 가정하고 프로그램을 구현한다. 단, 백업 및 다중화 대책을 마련하는 것은 지나치게 많은 품이 들기 때문에, 여기서는 인프라에 일시적인 장애가 발생할 수는 있지만 데이터는 영속적이라고
가정한다. (인프라 파괴 및 유실 시나리오 배제)

도메인 주도 설계를 도입하되, 부분적으로만 도입한다. (설계로 인해 실습을 못하는 문제 생김)

## 2. 도메인 주도 설계

### 2.1. 요구사항 정의 (Naive)

- 텔러 관리
	- 텔러 관리자는 텔러를 생성하고 삭제할 수 있다.
		- 텔러는 두 종류로, 직원 텔러와 자동 텔러(ATM)으로 나뉜다.
			- 직원 텔러의 특징으로, 직원 텔러는 여러 권한을 가질 수 있다.
				- 텔러 관리자는 직원 텔러에게 권한을 부여하거나 철회할 수 있다.
			- 자동 텔러의 특징으로, 자동 텔러는 자체적으로 현찰을 보관하는 금고를 가지고 있다.
				- 현찰은 자동 텔러의 금고와 은행 금고 사이를 이동할 수 있다.
				- "자동 텔러: 현찰 이동" 권한을 가진 직원 텔러가 이를 보증해야 한다.
			- 상호 배타성: 직원 텔러는 금고를 가질 수 없으며, 자동 텔러는 권한을 가질 수 없다.
	- 텔러 관리자는 각 텔러에게 서명용 비대칭키를 발급 및 재발급할 수 있다.
		- 텔러만이 서명용 개인키를 보유하며, 시스템은 서명용 공개키만을 갖는다.
		- 서명용 개인키를 활용해, 모든 텔러는 어떤 동작이 자신의 감독 하에 이루어졌음을 보증할 수 있다.
	- 텔러 관리자 및 텔러 본인은 텔러의 서명용 비대칭키를 무효화할 수 있다.
- 은행 금고 관리
	- 은행 금고는 은행이 가진 모든 현찰을 관리한다.
- 일일마감
	- 하루가 끝나면 시스템의 전날 상태 더하기 모든 거래가 오늘의 상태와 대차대조가 맞는지 확인해야 한다.
	- 일일마감에서 발견된 대차대조의 오차의 유무와 수치는 매일 레포트로 기록되어 시스템에 저장되어야 한다.
- 회원 관리
	- 회원 가입, 회원 탈퇴를 지원한다.
	- 회원 로그인, 회원 로그아웃을 지원한다.
- 계좌 관리
	- 회원은 계좌를 개설하거나 폐쇄할 수 있다.
	- 회원은 텔러의 중개를 통해 계좌에 현찰을 입금하거나 출금할 수 있다.
		- 입금과 출금을 중개할 수 있는 텔러는 모든 자동 텔러와 "회원: 입출금" 권한을 가진 텔러이다.
		- 모든 입금과 출금은 이를 중개할 수 있는 텔러가 보증해야 한다.
	- 회원은 보유한 계좌의 목록과 각 계좌의 잔액, 보류잔액, 거래기록을 조회할 수 있다.
	- 회원은 다른 계좌로 송금할 수 있다.
		- 의심스러운 송금 이외의 송금은 즉시 완료 처리되며, 의심스러운 송금은 지연 처리된다.
		- 의심스러운 송금의 조건은 다음과 같다.
			- 한 번에 고액을 송금한다.
			- 짧은 시간에 여러 번 송금한다.
			- 개설된 지 얼마 되지 않은 계좌로 송금한다.
			- 오랫동안 사용되지 않은 계좌로 송금한다.
		- 지연된 송금의 액수에 해당하는 만큼의 계좌 잔액은 보류 잔액으로 처리되어 계좌의 일반 잔액과는 구분된다.
			- 보류 잔액은 지연된 송금이 완료되기 전까지 어디로도 이동하거나 입출금될 수 없다.
		- 지연 처리된 송금은 "모니터링: 지연 송금" 권한을 가진 텔러가 이를 직접 승인하거나 거부하면 완료된다.
- 모니터링
	- 적절한 권한을 가진 텔러는 시스템의 다양한 이벤트를 실시간으로 조회할 수 있어야 한다.
		- "모니터링: 회원" 권한을 가진 텔러는 회원 관리 이벤트를 조회할 수 있다.
		- "모니터링: 계좌" 권한을 가진 텔러는 계좌 이벤트를 조회할 수 있다.
		- "모니터링: 텔러" 권한을 가진 텔러는 텔러 이벤트를 조회할 수 있다.
		- "모니터링: 지연 송금" 권한을 가진 텔러는 지연된 송금 요청을 확인하고, 이를 직접 승인하거나 거부할 수 있다.

### 2.2. 도메인 식별

- 핵심 도메인
	- 계좌 관리
- 지원 도메인
	- 회원 관리
- 일반 도메인
	- 모니터링

이를 그대로 바운디드 컨텍스트로 활용한다.

- 회원관리 컨텍스트
- 계좌관리 컨텍스트
- 모니터링 컨텍스트

### 2.3. 도메인 용어 정의

- 텔러관리 도메인: 텔러(Teller)
	- 직원텔러 (Employee Teller)
		- 권한 (Permission)
	- 자동텔러 (Auto Teller)
		- 금고 (Vault)
		- 현찰 (Cash)
			- 자동 텔러: 현찰 이동 (Auto Teller: Cash Movement)
	- 보증 (Certification)
		- 서명용 개인키 (Private Key), 공개키 (Public Key), 비대칭키 (Assymetric Key)
		- 무효화 (Invalidate)
	- 은행 금고 (Bank Vault)
- 일일마감 도메인: 일일마감 (Daily Closing)
	- 마감 상태 (Closed status)
	- 하루의 거래 (Daily Transactions)
	- 대차대조 (Reconciliation)
	- 오차 (Discrepancy)
	- 레포트 (Report)
- 회원관리 도메인: 회원(Member)
	- 가입(Register)
	- 탈퇴(Withdraw)
	- 로그인(Login)
	- 로그아웃(Logout)
- 계좌관리 도메인: 계좌(Account)
	- 개설(Open)
	- 폐쇄(Close)
	- 조회(Lookup)
		- 잔고 (Balance)
		- 보류잔고 (Pending Balance)
		- 거래기록 (Account History)
	- 실행(Execute)
	- 입금(Deposit)
	- 출금(Withdraw)
	- 송금(Transfer)
		- 의심스러운 송금 (Suspicious Transfer) -> 지연 처리 (Delayed Execute)
			- 고액 송금 (Large Transfer)
			- 잦은 송금 (Frequent Transfer)
			- 신규계좌로 송금 (To New Account Transfer)
			- 장기간 미사용 계좌로 송금 (Inactive Account Transfer)
		- 처리(Execute)
			- 지연 (Delay)
				- 승인 (Accept)
				- 거부 (Reject)
			- 완료 (Complete)
- 모니터링 도메인 (Monitoring)
	- 모니터링: 회원 (Monitoring: Member)
	- 모니터링: 계좌 (Monitoring: Account)
	- 모니터링: 텔러 (Monitoring: Teller)
	- 모니터링: 지연 송금 (Monitoring: Delayed Transfer)

## 3. 시스템 디자인

### 배포 디자인

```mermaid
flowchart TD
%% 서버 유닛
    MemberBackendServer["회원 백엔드 서버"]
    FrontendServer["대고객 프론트엔드 서버"]
    AccountBackendServer["계좌 백엔드 서버"]
    MonitoringBackendServer["모니터링 백엔드 서버"]
%% 영속성 유닛
    MemberDB[("회원 DB")]
    MessageBroker[/"메시지 큐"/]
    AccountDB[("계좌 DB")]
    MonitoringDB[("모니터링 DB")]
    AccountBackendServer -- " 계좌 데이터 영속성 " --> AccountDB
    MonitoringFrontendServer["모니터링 프론트엔드 서버"]
%% 대고객 UI와 서비스 연결
    FrontendServer -- " UI 요청 " --> MemberBackendServer
    FrontendServer -- " UI 요청 " --> AccountBackendServer
%% 백엔드와 DB 연결
    MemberBackendServer -- " 회원 데이터 영속성 " --> MemberDB
%% 이벤트 메시지 흐름
    MemberBackendServer -- " 이벤트 생산 " --> MessageBroker
    AccountBackendServer -- " 이벤트 생산 " --> MessageBroker
    MessageBroker -- " 이벤트 소모 " --> MonitoringBackendServer
%% 모니터링 UI와 서비스 연결
    MonitoringFrontendServer -- " UI 요청 " --> MonitoringBackendServer
%% 모니터링 DB 연결
    MonitoringBackendServer -- " 모니터링 데이터 영속성 " --> MonitoringDB
%% 스타일 및 클래스 지정
    classDef server fill: #fffde7, stroke: #fbc02d, stroke-width: 2px;
    class FrontendServer server;
    class MemberBackendServer server;
    class AccountBackendServer server;
    class MonitoringBackendServer server;
    class MonitoringFrontendServer server;
    classDef storage fill: #e3f2fd, stroke: #1565c0, stroke-width: 2px;
    class MemberDB storage;
    class AccountDB storage;
    class MonitoringDB storage;
    class MessageBroker storage;

```

### 구성요소 디자인

```mermaid
flowchart TD
%% Unified Customer Frontend
    subgraph CustomerFrontendServer["대고객 프론트엔드 서버"]
        MemberUI["회원 UI"]
        AccountUI["계좌 UI"]
    end

%% Member Backend
    subgraph MemberBackendServer["회원 백엔드 서버"]
        MemberService["회원 서비스"]
        MemberDB[("회원 DB")]
        MemberOutboxService["회원 아웃박스 서비스"]
        MemberOutboxDB[("회원 아웃박스 DB")]
        MemberUI -- " API " --> MemberService
        MemberService -- " 회원 관리 " --> MemberDB
        MemberService -- " 이벤트 기록 " --> MemberOutboxDB
        MemberOutboxDB -- " 이벤트 획득 " --> MemberOutboxService
    end

%% Account Backend
    subgraph AccountBackendServer["계좌 백엔드 서버"]
        AccountService["계좌 서비스"]
        AccountDB[("계좌 DB")]
        AccountOutboxService["계좌 아웃박스 서비스"]
        AccountOutboxDB[("계좌 아웃박스 DB")]
        AccountUI -- " API " --> AccountService
        AccountService -- " 계좌 관리 " --> AccountDB
        AccountService -- " 이벤트 기록 " --> AccountOutboxDB
        AccountOutboxDB -- " 이벤트 획득 " --> AccountOutboxService
    end

%% 메시지 브로커
    MessageBroker[/"메시지 브로커"/]
    MessageBroker -- " 이벤트 수신 " --> MonitoringConsumer
    MemberOutboxService -- " 이벤트 전송 " --> MessageBroker
    AccountOutboxService -- " 이벤트 전송 " --> MessageBroker
%% Monitoring Backend
    subgraph MonitoringBackendServer["모니터링 백엔드 서버"]
        MonitoringConsumer["모니터링 이벤트 소비자"]
        MonitoringService["모니터링 서비스"]
        MonitoringDB[("모니터링 DB")]
        MonitoringConsumer -- " 이벤트 기록 " --> MonitoringDB
        MonitoringService -- " 모니터링 활동 " --> MonitoringDB
    end

%% Monitoring Frontend
    subgraph MonitoringFrontendServer["모니터링 프론트엔드 서버"]
        MonitoringUI["모니터링 UI"]
        MonitoringUI -- " UI " --> MonitoringService
    end

%% 스타일 및 클래스 지정
    classDef server fill: #fffde7, stroke: #fbc02d, stroke-width: 2px;
    class CustomerFrontendServer server;
    class MemberBackendServer server;
    class AccountBackendServer server;
    class MonitoringBackendServer server;
    class MonitoringFrontendServer server;
    classDef storage fill: #e3f2fd, stroke: #1565c0, stroke-width: 2px;
    class MessageBroker storage;
    class MemberDB storage;
    class AccountDB storage;
    class MemberOutboxDB storage;
    class AccountOutboxDB storage;
    class MonitoringDB storage;
    classDef component fill: #ffe0b2, stroke: #ef6c00, stroke-width: 2px;
    class MemberUI component;
    class AccountUI component;
    class MonitoringUI component;
    class MemberFrontendService component;
    class AccountFrontendService component;
    class MemberService component;
    class AccountService component;
    class MemberOutboxService component;
    class AccountOutboxService component;
    class MonitoringConsumer component;
    class MonitoringService component;

```

- 선택: 메시지 브로커를 통해 회원/계좌 시스템과 모니터링 시스템을 분리.
	- 근거: 모니터링 이벤트 확인은 중요하지만 결과적 일관성으로 충분. 시스템 결합도를 낮추기에 유리한 디자인.
	- 비용: 모니터링 시스템의 즉시 일관성이 결과적 일관성으로 격하.
- 선택: 아웃박스 패턴 도입으로 메시지 발행
	- 근거: DBMS와 Kafka 양측 모두를 포함하는 신뢰성 있는 Dual-write의 어려움.
	- 비용: 서비스-MQ 구조가 서비스-DBMS-서비스-MQ 구조로 복잡성 증가.

### 인터페이스 디자인

#### 회원 서비스

| 엔드포인트                     | 메서드    | 설명     |
|---------------------------|--------|--------|
| `/accounts`               | GET    | 계좌목록조회 |
| `/accounts`               | POST   | 계좌개설   |
| `/accounts/{id}`          | GET    | 계좌조회   |
| `/accounts/{id}`          | DELETE | 계좌폐쇄   |
| `/accounts/{id}/deposit`  | POST   | 입출금실행  |
| `/accounts/{id}/withdraw` | POST   | 입출금실행  |

| 엔드포인트             | 메서드  | 설명     |
|-------------------|------|--------|
| `/transfers`      | POST | 이체실행   |
| `/transfers/{id}` | POST | 고액송금처리 |

모든 엔드포인트는 멱등하다.

#### 모니터링 서비스

| 엔드포인트                         | 메서드 | 설명            |
|-------------------------------|-----|---------------|
| `/monitoring`                 | GET | 최근 이벤트 조회     |
| `/monitoring/large-transfers` | GET | 대기 중인 고액송금 조회 |

모든 엔드포인트는 멱등하다.

<!-- TODO ### 데이터 디자인 -->
