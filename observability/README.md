# Loki + Grafana 로그 플랫폼 배포 가이드

E2.1.Micro #2 인스턴스에 Loki + Grafana를 띄우고, ARM/E2 #1에서 Promtail로 로그를 수집한다.

## 사전 준비

### 1. OCI에 새 E2.1.Micro 인스턴스 생성
- 같은 VCN/Subnet 사용 (ARM, E2 #1과 통신해야 함)
- VCN 내부 IP를 메모해둘 것 (예: `10.0.0.150`) — Promtail 설정에 필요
- 보안 목록 (E2 #2):
  - Ingress 22/TCP ← 본인 IP (SSH)
  - Ingress 80/TCP ← 0.0.0.0/0 (HTTPS 리다이렉트, certbot)
  - Ingress 443/TCP ← 0.0.0.0/0 (Grafana)
  - Ingress 3100/TCP ← VCN CIDR (예: 10.0.0.0/16) — Loki API, **외부 차단**

### 2. DNS (Gabia)
- A 레코드: `grafana` → E2 #2 공인 IP

### 3. 강력한 Grafana admin 비밀번호 준비

---

## E2 #2 (Loki/Grafana) 셋업

### 1. Docker 설치
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
exit  # 재접속
```

### 2. 디렉토리 생성
```bash
mkdir -p ~/observability
```

### 3. 파일 전송 (로컬 PowerShell에서)
```powershell
$KEY="C:\Users\admin\Desktop\heehun-key-pair.pem"
$E2_2_IP="<E2 #2 공인 IP>"

scp -i $KEY observability/docker-compose.yml      ubuntu@${E2_2_IP}:~/observability/
scp -i $KEY observability/loki-config.yml         ubuntu@${E2_2_IP}:~/observability/
scp -i $KEY observability/nginx.conf              ubuntu@${E2_2_IP}:~/observability/
scp -i $KEY observability/nginx-init.conf        ubuntu@${E2_2_IP}:~/observability/
```

### 4. .env 파일 생성 (E2 #2 서버에서)
```bash
cd ~/observability
cat > .env << EOF
GF_ADMIN_PASSWORD=<강력한 비밀번호>
EOF
chmod 600 .env
```

### 5. 인증서 발급 (init nginx로 잠시 띄우기)
```bash
cd ~/observability

# 임시로 init 설정 사용
mv nginx.conf nginx.conf.real
cp nginx-init.conf nginx.conf

# nginx만 띄움
docker compose up -d nginx

# 확인
curl http://localhost
# → "nginx is running"

# 외부에서도 (브라우저) http://grafana.sqldpass.com 확인

# 인증서 발급
docker compose run --rm --entrypoint certbot certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  -d grafana.sqldpass.com \
  --email heehun3658@gmail.com \
  --agree-tos --no-eff-email

# 진짜 nginx.conf로 복원
mv nginx.conf.real nginx.conf

# 전체 스택 가동
docker compose up -d

# 상태 확인
docker compose ps
```

### 6. Grafana 접속
- `https://grafana.sqldpass.com`
- ID: `admin`
- PW: `.env`에 설정한 값

---

## ARM 백엔드 인스턴스에 Promtail 설치

### 1. promtail-config.yml의 LOKI_HOST 교체
로컬에서 `observability/promtail-config.yml`의 `<LOKI_HOST>`를 E2 #2의 **VCN 내부 IP**로 교체.

```yaml
clients:
  - url: http://10.0.0.150:3100/loki/api/v1/push   # ← 실제 IP
```

### 2. ARM 서버로 전송
```powershell
$KEY="C:\Users\admin\Desktop\heehun-key-pair.pem"
$ARM_IP="146.56.104.152"

scp -i $KEY observability/promtail-compose.yml ubuntu@${ARM_IP}:~/promtail/
scp -i $KEY observability/promtail-config.yml  ubuntu@${ARM_IP}:~/promtail/
```

### 3. ARM에서 실행
```bash
ssh ubuntu@146.56.104.152
mkdir -p ~/promtail
cd ~/promtail
docker compose -f promtail-compose.yml up -d
docker compose -f promtail-compose.yml logs -f
```

`level=info ... file_target_manager` 같은 로그가 보이면 정상.

---

## E2 #1 (nginx) 인스턴스에도 Promtail 설치

위와 동일한 절차. `158.179.175.204`에 `~/promtail/` 만들고 같은 파일 배포.

---

## Grafana 데이터소스 설정

1. `https://grafana.sqldpass.com` 로그인
2. **Connections** → Data sources → Add data source
3. **Loki** 선택
4. URL: `http://loki:3100` (같은 docker-compose 내부 통신)
5. **Save & test** → 초록 체크 확인

## 사용 (Explore에서 LogQL)

### 자주 쓰는 쿼리

```logql
# 백엔드 전체 로그
{container="sqldpass-app"}

# 백엔드 ERROR만
{container="sqldpass-app"} |= "ERROR"

# 슬로우 쿼리 (1초 이상)
{container="sqldpass-app"} |~ "SlowQuery|HHH000487"

# nginx 5xx 응답
{container="nginx-proxy"} |~ " 5\\d\\d "

# nginx 액세스 전체
{container="nginx-proxy"}

# 특정 시간 범위 + 키워드
{container="sqldpass-app"} |= "exception" | logfmt
```

자주 보는 쿼리는 **Save** → 대시보드 패널로 만들면 다음에 빠르게 접근 가능.

---

## 관리

### 디스크 사용량 확인 (E2 #2)
```bash
docker exec loki du -sh /loki
```

### Loki 재시작 (설정 변경 후)
```bash
cd ~/observability
docker compose restart loki
```

### Retention 변경
`loki-config.yml`의 `retention_period: 336h`를 수정 후:
```bash
docker compose restart loki
```

### Grafana 비밀번호 변경
1. `.env` 파일의 `GF_ADMIN_PASSWORD` 수정
2. `docker compose up -d grafana` (재생성 안 됨, 수동 변경 필요)
3. 또는 Grafana UI → Profile → Change password
