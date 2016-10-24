package me.mrkirby153.kcuhc.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.junit.Assert;
import org.junit.Test;

public class UtilChatTest {

    @Test
    public void generateBoldChat() throws Exception {

    }

    @Test
    public void generateError() throws Exception {
        BaseComponent error = UtilChat.generateError("Testing");
        Assert.assertEquals("error> testing", error.toPlainText().toLowerCase());
    }

    @Test
    public void generateFormattedChat() throws Exception {
        BaseComponent formatted = UtilChat.generateFormattedChat("testing", ChatColor.BLUE, 8);
        BaseComponent expected = new TextComponent("testing");
        expected.setBold(true);
        expected.setColor(ChatColor.BLUE);
        Assert.assertEquals(expected.toLegacyText(), formatted.toLegacyText());
    }

    @Test
    public void generateItalicChat() throws Exception {
        BaseComponent formatted = UtilChat.generateItalicChat("testing", ChatColor.BLUE);
        BaseComponent expected = new TextComponent("testing");
        expected.setItalic(true);
        expected.setColor(ChatColor.BLUE);
        Assert.assertEquals(expected.toLegacyText(), formatted.toLegacyText());
    }

    @Test
    public void generateLegacyError() throws Exception {
        Assert.assertEquals(ChatColor.BLUE + "error> " + ChatColor.GRAY + "testing", UtilChat.generateLegacyError("testing").toLowerCase());
    }

    @Test
    public void message() throws Exception {
        Assert.assertEquals(ChatColor.BLUE + "> " + ChatColor.GRAY + "testing", UtilChat.message("testing"));
    }

}