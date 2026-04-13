---
name: add-exam
description: 새로운 자격증 시험 과목을 추가한다. "새 과목 추가", "시험 추가", "자격증 추가", "컴활 2급 추가", "새 시험 등록" 등의 요청이 있을 때 사용. 백엔드(ExamType, 마이그레이션, 생성기, 시드, 프롬프트) + 프론트엔드(NavBar, 모의고사, 풀이, 오답노트) 전체를 구현한다.
---

# 새 자격증 시험 과목 추가 스킬

## 역할

사용자가 새로운 자격증 시험을 요청하면, 필요한 정보를 단계별로 수집하고 백엔드+프론트엔드 전체를 구현한다.

## 워크플로우

### Step 1: 시험 스펙 수집

사용자에게 아래 정보를 **순서대로** 물어본다. 이미 알려준 정보는 건너뛴다.

**필수 정보:**
1. 시험 정식 명칭 (예: "컴퓨터활용능력 2급 필기")
2. 총 문항 수 (예: 40문항)
3. 과목 구성 (예: 컴퓨터 일반 20문항 + 스프레드시트 20문항)
4. 시험 시간 (예: 40분)
5. 합격 기준 (예: 평균 60점 + 과목별 40점)
6. 시험 형태 (4지선다 / 단답형+서술형 / 혼합)

**선택 정보 (있으면 좋음):**
7. 과목별 출제 토픽과 빈도 (높음/보통/낮음)
8. 2024년 이후 개편 사항
9. 기존 시험과 겹치는 과목 여부

### Step 2: 시드 문제 확보

시드 확보 방식을 사용자에게 물어본다:

**옵션 A — 사용자가 직접 제공**
- JSON 형식으로 시드 문제를 받는다
- 형식: `{ subject, topic, difficulty, content, correctOption, explanation }`

**옵션 B — LLM 프롬프트 생성**
- Step 1에서 수집한 정보로 시드 생성용 프롬프트를 작성한다
- 프롬프트에 포함할 것:
  - 토픽 분류표 (출제 빈도 포함)
  - 토픽별 상/중/하 난이도 문제 각 1개씩 요청
  - JSON 출력 형식 지정
  - 시험 형태에 맞는 문제 형식 (4지선다 or 단답형)
  - 2024 개편 반영 지시
- 사용자가 웹 Claude/ChatGPT에서 실행 후 결과를 붙여넣으면 Step 3으로

**옵션 C — 내장 지식 기반 생성**
- 내가 알고 있는 해당 시험 정보로 직접 시드를 생성한다
- 정확도 보장이 안 되므로 사용자 검수 필요

### Step 3: 출제 분포 결정

사용자에게 토픽별 문항 수를 물어본다.

- 토픽 수와 문항 수가 비슷하면 → 균등 랜덤 (컴활 1급 방식)
- 토픽별 출제 빈도 차이가 크면 → 가중 추출 (컴활 2급 방식)
- 과목 내 소분류가 있으면 → SubGroup (정처기 필기 방식)

사용자가 "이 토픽은 4문제, 저 토픽은 1문제" 식으로 분포를 주면 가중 추출로 구현.
분포를 모르면 일단 균등 랜덤으로 구현하고 나중에 조정.

### Step 4: 백엔드 구현

기존 시험 중 가장 유사한 구현체를 복제하여 수정한다.

**유사도 판단 기준:**
- 4지선다 객관식 → ComputerLiteracyMockExamCreator 참고
- 단답형+서술형 → EngineerMockExamCreator 참고
- 100문항 5과목 → EngineerWrittenMockExamCreator 참고

**생성/수정 파일 체크리스트:**
- [ ] `ExamType.java` — enum 값 추가
- [ ] `V{next}__*.sql` — 루트 과목 + 하위 과목 마이그레이션
- [ ] `{Exam}TopicExamples.java` — 시드 문제 파일 생성
- [ ] `{Exam}MockExamCreator.java` — 모의고사 생성기 생성
- [ ] `SubTopicCatalog.java` — 토픽 목록 추가 + forSubject switch 추가
- [ ] `AiProvider.java` — generate 메서드 추가
- [ ] `PromptBuilder.java` — 시스템 프롬프트 + 유저 프롬프트 빌더 추가
- [ ] `PublicContentService.java` — slug/name/description 상수 + listCerts + listCategories + 라우팅
- [ ] `AdminQuestionService.java` — 검증 분기 (EXCLUDED_ROOTS, triage switch, resolveExamType)
- [ ] `MockExamService.java` — create switch에 case 추가 + 의존성 주입

### Step 5: 프론트엔드 구현

**생성/수정 파일 체크리스트:**
- [ ] `mockExamApi.ts` — ExamType 유니온 추가
- [ ] `adminApi.ts` — VerificationExamType, AdminMockExam.examType, CreateMockExamType, ExportExamType 추가
- [ ] `publicApi.ts` — CertSlug 추가
- [ ] `NavBar.tsx` — CertKey + CERT_OPTIONS 추가 (색상 결정)
- [ ] `solve/page.tsx` — cert tone + detectCertTone + certKeyToLabel 추가
- [ ] `mock-exams/page.tsx` — 탭 + 필터 추가
- [ ] `mock-exams/[id]/page.tsx` — EXAM_TIME_MINUTES 추가
- [ ] `admin/mock-exams/page.tsx` — CERTS 배열 추가
- [ ] `learn/[cert]/page.tsx` — CERT_META + certDisplayName 추가
- [ ] `wrong-answers/page.tsx` — CertKey + CERT_META + detectCertFromRootName 추가
- [ ] `admin/questions/page.tsx` — exam type 관련 추가

### Step 6: 빌드 검증

```bash
cd backend && ./gradlew compileJava compileTestJava
cd backend && ./gradlew test
```

컴파일 에러가 있으면 수정. 테스트 실패가 있으면 기존 테스트의 enum switch 누락 등을 확인.

### Step 7: 커밋 + 푸시

모든 파일을 하나의 커밋으로 묶어서 롤백 용이하게:
```
feat: {시험명} 전체 구현
```

## 색상 컨벤션

기존 시험 색상:
- SQLD: amber
- 정처기 실기: emerald
- 정처기 필기: rose
- 컴활 1급: sky
- 컴활 2급: indigo

새 시험은 위와 겹치지 않는 Tailwind 색상을 선택한다.

## 주의사항

- EngineerExamTemplate enum은 기존 모의고사 호환을 위해 삭제하지 않는다
- 마이그레이션 번호는 `ls backend/src/main/resources/db/migration/ | sort -V | tail -1`로 확인
- 시드 문제의 정확도는 AI가 보장할 수 없으므로, 반드시 사용자 검수 또는 AI 검증 파이프라인 통과 필요
- 기존 시험의 SQLD_EXCLUDED_ROOTS에 새 시험 루트를 추가해야 SQLD 검증에서 제외됨
