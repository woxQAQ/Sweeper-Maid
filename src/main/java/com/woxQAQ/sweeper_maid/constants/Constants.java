package com.woxQAQ.sweeper_maid.constants;

import java.util.Arrays;
import java.util.List;

import net.minecraft.SharedConstants;

public class Constants {
    private static final int TICKS_PER_SECOND = SharedConstants.TICKS_PER_SECOND;

    private static final int CountDown_15 = 15 * TICKS_PER_SECOND;
	private static final int CountDown_30 = 30 * TICKS_PER_SECOND;
	private static final int CountDown_60 = 60 * TICKS_PER_SECOND;
	public static final List<Integer> StandardCountDown = Arrays.asList(CountDown_60, CountDown_30, CountDown_15);
}
