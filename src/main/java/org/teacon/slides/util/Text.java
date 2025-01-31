package org.teacon.slides.util;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

public class Text {

	public static MutableText translatable(String text, Object... objects) {
		return new TranslatableText(text, objects);
	}

	public static MutableText literal(String text) {
		return new LiteralText(text);
	}
}
