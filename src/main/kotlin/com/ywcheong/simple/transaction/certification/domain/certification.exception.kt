package com.ywcheong.simple.transaction.certification.domain

import com.ywcheong.simple.transaction.common.exception.UserFaultException

sealed class CertificationException(msg: String) : UserFaultException(msg)
class BadFormatCertificationException : CertificationException("보증 서명의 양식이 잘못되었습니다.")
class BadSignatureCertificationException : CertificationException("보증 서명이 손상되어 신뢰할 수 없습니다.")