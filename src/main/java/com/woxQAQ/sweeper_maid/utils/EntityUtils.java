package com.woxQAQ.sweeper_maid.utils;

import com.woxQAQ.sweeper_maid.config.SMCommonConfig;

import net.minecraft.world.entity.item.ItemEntity;

public class EntityUtils {

	public static boolean inBlacklist(ItemEntity entity) {
		return SMCommonConfig.SWEEP_BLACKLIST.get().contains(
				entity.getType().toString());
	}
}
