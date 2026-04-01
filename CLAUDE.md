# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

sqld 문제집 사이트 (문제 제공, 풀이 제공)

## Repository Structure

모노레포 구조:
- `backend/` — Spring Boot API 서버 (Java 21, Gradle)
- `frontend/` — 프론트엔드 (TBD)

## Common Rules

- Windows 환경. Git Bash/WSL에서는 Unix 명령, PowerShell/CMD에서는 Windows 명령 사용
- 실제 DB 계정 정보는 커밋하지 않음. 환경별 설정은 프로필 분리 방식으로 관리
- 커밋 메시지는 짧은 명령형 문장 (예: `Add login validation`)
