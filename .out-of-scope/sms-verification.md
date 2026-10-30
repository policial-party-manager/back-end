# SMS Verification Code Channel

The platform does **not** send SMS verification codes. Phone-number login stays structurally available but unusable in practice: the code is generated and stored, then only logged — never delivered.

## Why this is out of scope

Requiring an SMS carrier means taking on an external dependency the project deliberately avoids:

- **Cost & procurement**: every verification message has a per-message fee and requires a registered business entity plus carrier template approval (`签名` + `模板报备`). That is an organisational commitment, not a code change.
- **Risk surface**: an SMS-sending endpoint is a favourite abuse target (SMS bombing, cost amplification). Guarding it properly needs per-phone and per-IP rate limiting, CAPTCHA, and monitoring — infrastructure this repo has no other reason to run.
- **No audience yet**: the account base has **0 users with a phone number on file** (1 user total, no `phone` filled). The frontend does call `/v1/auth/login/phone`, but nothing can actually log in through it.
- **Email already covers the same need**: `POST /api/v1/auth/login/email` plus `POST /api/v1/auth/verifyCode` delivers the same "passwordless code login" flow through SMTP, which this repo already configured (`spring.mail.*`, degrades to log output when unset).

The code seam is intentionally left in place — see the comment in `AuthServiceImpl.verifyCode`:

```java
} else {
    // 短信通道尚未接入（留白）：验证码已存 Redis，后续接入短信服务时在此处真实发送
    log.warn("[短信通道未接入] 目标手机号: {}, 验证码: {}", phone, code);
}
```

`CodeUtil` is channel-agnostic by design, so a future integration is a single-call addition, not a refactor. Keeping the seam costs nothing; wiring a carrier does.

## What is NOT rejected

- **Email verification codes** — implemented and in use.
- **Phone as a profile field** — stored in `tb_user_detail.phone`, editable via the personal centre.
- **Phone-number login *structure*** — the endpoint, DTO validation and Redis flow already exist and stay.

## Prior requests

- #37: "decide: 短信验证码通道是否接入及供应商" — triaged and declined by maintainer, 2026-10-30
