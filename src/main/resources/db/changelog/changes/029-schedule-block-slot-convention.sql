-- BD-272: 슬롯 규약 통일 (end_slot 을 1..48 로)
--         기존 p_schedule_block 은 end_slot 을 0..47 로 두어 자정 종료를
--         [end_date = 다음날, end_slot = 0] 으로 표현했다.
--         availability 계열(WeeklyRule, AvailabilityException)과 ScheduleBoard 는
--         end_slot 을 1..48 로 쓰고 있어, 같은 시각이 두 가지로 표현되는 문제가 있었다.
--         자정 표현을 [end_date = 당일, end_slot = 48] 하나로 정규화한다.

UPDATE public.p_schedule_block
SET end_date = end_date - INTERVAL '1 day',
    end_slot = 48
WHERE end_slot = 0;
