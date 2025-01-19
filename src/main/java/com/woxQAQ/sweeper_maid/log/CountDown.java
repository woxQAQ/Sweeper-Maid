package com.woxQAQ.sweeper_maid.log;


import com.woxQAQ.sweeper_maid.config.SMCommonConfig;
import com.woxQAQ.sweeper_maid.constants.Constants;

import net.minecraft.SharedConstants;

public class CountDown {
    private int tickRemain;

    private static final int MaxLogCountDown = 60 * SharedConstants.TICKS_PER_SECOND;

    public CountDown(int tickRemain) {
        this.tickRemain = tickRemain;
    }

    private boolean isStandardCountDown() {
        return Constants.StandardCountDown.contains(this.tickRemain);
    }

    private boolean isShortCountDown() {
        return this.tickRemain % SharedConstants.TICKS_PER_SECOND > 0
                && this.tickRemain / SharedConstants.TICKS_PER_SECOND <= 10;
    }


    public String getMessage() {
        if (this.tickRemain > MaxLogCountDown) {
            return "";
        }

        if (this.isStandardCountDown()) {
            return SMCommonConfig.MESSAGE_BEFORE_SWEEP_15_30_60.get().replaceAll("\\$1",
                    String.valueOf(this.tickRemain / SharedConstants.TICKS_PER_SECOND));
        }
        if (this.isShortCountDown()) {
            return SMCommonConfig.MESSAGE_BEFORE_SWEEP_1_10.get().replaceAll("\\$1",
                    String.valueOf(this.tickRemain / SharedConstants.TICKS_PER_SECOND));
        }
        return "";
    }
}
