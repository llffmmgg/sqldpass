# nginx 리버스 프록시 배포 가이드

E2.1.Micro 인스턴스에 nginx + Let's Encrypt를 설정하는 절차.

## 사전 조건

- E2.1.Micro 인스턴스 (158.179.175.204)
- 도메인 DNS 설정 완료: `api.sqldpass.com` A 레코드 → `158.179.175.204`
- ARM 백엔드 인스턴스 VCN 내부 IP: `10.0.0.72`

## 1. E2.1.Micro 서버 준비

```bash
# Docker 설치
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# SSH 재접속

# 디렉토리 생성
mkdir -p ~/proxy
```

## 2. 파일 전송 (로컬에서)

```bash
# 로컬에서 E2 서버로 nginx 설정 파일 복사
scp -i <키파일> proxy/nginx-init.conf proxy/nginx.conf proxy/docker-compose.yml ubuntu@158.179.175.204:~/proxy/
```

## 3. 인증서 발급 (E2 서버에서, 1회성)

```bash
cd ~/proxy

# 임시로 nginx-init.conf를 사용 (HTTP만)
cp nginx-init.conf nginx.conf.tmp
mv nginx.conf nginx.conf.bak
mv nginx.conf.tmp nginx.conf

# nginx 시작
docker compose up -d nginx

# 인증서 발급
docker compose run --rm certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  -d api.sqldpass.com \
  --email heehun3658@gmail.com \
  --agree-tos --no-eff-email

# 진짜 nginx.conf로 복원
mv nginx.conf.bak nginx.conf

# nginx + certbot 모두 시작
docker compose up -d
```

## 4. OCI 보안 목록 설정

**E2.1.Micro 인스턴스 보안 목록:**
- Ingress 80 (TCP) ← 0.0.0.0/0 (HTTP, certbot 갱신용)
- Ingress 443 (TCP) ← 0.0.0.0/0 (HTTPS)

**ARM 백엔드 인스턴스 보안 목록:**
- Ingress 8080 (TCP) ← VCN CIDR만 (예: 10.0.0.0/16)
- 기존 8080 ← 0.0.0.0/0 규칙은 **제거**

## 5. Vercel 환경변수 변경

- Vercel Dashboard → Project Settings → Environment Variables
- `NEXT_PUBLIC_API_URL` 값을 `https://api.sqldpass.com`으로 변경
- Production 재배포

## 6. 검증

```bash
# DNS 확인
dig api.sqldpass.com

# HTTP → HTTPS 리다이렉트 확인
curl -I http://api.sqldpass.com

# HTTPS 정상 동작 확인
curl https://api.sqldpass.com/actuator/health

# ARM 인스턴스 8080 외부 접근 차단 확인 (timeout 나야 정상)
curl --max-time 5 http://146.56.104.152:8080/actuator/health
```
