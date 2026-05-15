"use client";

import { useCallback, useRef } from "react";

// 같은 값으로 thresholdMs 안에 두 번 호출되면 onDouble, 아니면 onSingle.
// native onDoubleClick 은 모바일 큰 박스에서 두 번째 탭 위치 톨러런스가 좁아 들쭉날쭉하므로
// onClick 기반으로 직접 더블탭 감지한다.
export function useDoubleTap<T>(
  onSingle: (value: T) => void,
  onDouble: (value: T) => void,
  thresholdMs = 350,
) {
  const lastRef = useRef<{ value: T; time: number } | null>(null);

  return useCallback(
    (value: T) => {
      const now = Date.now();
      const last = lastRef.current;
      if (last && last.value === value && now - last.time < thresholdMs) {
        lastRef.current = null;
        onDouble(value);
        return;
      }
      lastRef.current = { value, time: now };
      onSingle(value);
    },
    [onSingle, onDouble, thresholdMs],
  );
}
