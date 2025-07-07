package net.JordanRiver.KisekiLegend.client.screen;

import net.JordanRiver.KisekiLegend.menu.OrbmentMenu;
import net.JordanRiver.KisekiLegend.orbal.ArtsRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class OrbmentScreen extends AbstractContainerScreen<OrbmentMenu> {
    private static final int PANEL_WIDTH   = 480;
    private static final int PANEL_HEIGHT  = 256;

    // arts panel position & size
    private static final int ARTS_X        = 8;
    private static final int ARTS_Y        = 140;
    private static final int ARTS_W        = 440;
    private static final int ARTS_H        = 100;

    // scrollbar inside that panel
    private static final int SCROLL_W      = 6;
    private static final int SCROLL_MARGIN = 4;

    private int centerX, centerY;
    private int scrollOffset = 0;
    private static final int MAX_ARTS_VISIBLE = 5;
    private List<ArtsRegistry.ArtDefinition> artsUnlocked = new ArrayList<>();

    // thumb‐drag state
    private boolean draggingThumb = false;

    public OrbmentScreen(OrbmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width  - this.imageWidth)  / 2;
        this.topPos  = (this.height - this.imageHeight) / 2;
        centerX = leftPos + imageWidth  / 2;
        centerY = topPos  + 100;
    }

    private void clampScroll() {
        int maxOffset = Math.max(0, artsUnlocked.size() - MAX_ARTS_VISIBLE);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private void updateUnlockedArts() {
        Map<String,Integer> totals = calculateElementalTotals();
        artsUnlocked.clear();
        for (var art : ArtsRegistry.ALL_ARTS) {
            boolean ok = true;
            for (var cost : art.elementCost().entrySet()) {
                if (totals.getOrDefault(cost.getKey().toLowerCase(), 0) < cost.getValue()) {
                    ok = false;
                    break;
                }
            }
            if (ok) artsUnlocked.add(art);
        }
        clampScroll();
    }

    private Map<String,Integer> calculateElementalTotals() {
        this.menu.getOrbmentComponent().recalculate();
        int[] sepith = this.menu.getOrbmentComponent().getSepithCounts();
        String[] names = {"earth","water","wind","fire","space","mirage","time"};
        Map<String,Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            totals.put(names[i], sepith[i]);
        }
        return totals;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mouseX, int mouseY) {
        updateUnlockedArts();

        // inventory box
        drawPanel(gui, leftPos + 8, topPos + 8, 172, 122, 0xFF4A3828);

        // arts box (behind core)
        int ax = leftPos + ARTS_X, ay = topPos + ARTS_Y;
        drawPanel(gui, ax, ay, ARTS_W, ARTS_H, 0xFF392418);

        // core & sepith
        drawPanel(gui, centerX - 64, centerY - 64, 128, 128, 0xFF3E2E20);
        drawPanel(gui, leftPos + 340, topPos + 8, 130, 112, 0xFF493420);

        // headings
        gui.drawString(this.font, "Orbment Inventory", leftPos + 16,           topPos + 12, 0xFFDDAA);
        gui.drawString(this.font, "Orbment Core",      centerX - 36,          centerY - 54, 0xFFD700);
        gui.drawString(this.font, "Sepith Totals",     leftPos + 348,         topPos + 16,   0xFFFFBB);
        gui.drawString(this.font, "Arts Unlocked",     ax + 8,               ay + 4,        0x77DDFF);

        // draw scrollbar track + thumb
        int trackX  = ax + ARTS_W - SCROLL_MARGIN - SCROLL_W;
        int trackY  = ay + SCROLL_MARGIN;
        int trackH  = ARTS_H - SCROLL_MARGIN*2;
        int maxOff  = Math.max(1, artsUnlocked.size() - MAX_ARTS_VISIBLE);
        // track background
        gui.fill(trackX, trackY, trackX + SCROLL_W, trackY + trackH, 0xFF2E1F14);
        // thumb size + position
        int thumbH  = Math.max(10, trackH * MAX_ARTS_VISIBLE / Math.max(artsUnlocked.size(), MAX_ARTS_VISIBLE));
        int thumbY  = trackY + (trackH - thumbH) * scrollOffset / maxOff;
        gui.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH, 0xFF704214);

        // arts table @75% scale, dropped 10px
        float scale = 0.75f;
        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1f);

        int sx = (int)((ax + 8) / scale);
        int sy = (int)((ay + 30) / scale);

        // header columns
                gui.drawString(this.font, "Name",   sx,                        sy,                   0xFFFFAA);
               gui.drawString(this.font, "Elem",   sx + (int)(80/scale),      sy,                   0xFFFFAA); // ↑ shifted right
               gui.drawString(this.font, "Cost",   sx + (int)(140/scale),     sy,                   0xFFFFAA); // ↑ shifted right
        gui.drawString(this.font, "Time",   sx + (int)(180/scale),     sy, 0xFFFFAA); // shifted right
              gui.drawString(this.font, "Power",  sx + (int)(230/scale),     sy, 0xFFFFAA); // shifted right
        // moved left from 320→260:
        gui.drawString(this.font, "Effect", sx + (int)(260/scale),     sy,                   0xFFFFAA);

        if (artsUnlocked.isEmpty()) {
            gui.drawString(this.font, "<no arts unlocked>",
                    sx, sy + (int)(12/scale), 0x888888
            );
        } else {
            for (int i = 0; i < MAX_ARTS_VISIBLE && i + scrollOffset < artsUnlocked.size(); i++) {
                var art = artsUnlocked.get(i + scrollOffset);
                int y = sy + (int)((12 + i * 10) / scale);

                gui.drawString(this.font, truncate(art.name(),                   14), sx,                    y, 0xFFFFFF);
                                gui.drawString(this.font, truncate(formatElem(art.elementCost()), 12), sx + (int)(80/scale),   y, 0x99FF99);
                                gui.drawString(this.font, truncate(art.epCost(),                  8),  sx + (int)(140/scale),  y, 0xFFDDDD);
                gui.drawString(this.font, truncate(art.castTime(), 6),
                                             sx + (int)(180/scale), y, 0xFFFFBB); // shifted right
                             gui.drawString(this.font, truncate(art.power(), 4),
                                     sx + (int)(230/scale), y, 0xFFAAAA); // shifted right
                gui.drawString(this.font, truncate(art.effectDescription(),      40),  sx + (int)(260/scale), y, 0xAAAAFF);
            }
        }
        gui.pose().popPose();

        // core slots
        for (int i = 0; i < 6; i++) {
            double ang = 2*Math.PI*i/6 - Math.PI/2;
            int sx2 = (int)(centerX + 40*Math.cos(ang)) - 9;
            int sy2 = (int)(centerY + 40*Math.sin(ang)) - 9;
            gui.fill(sx2, sy2, sx2+18, sy2+18, 0xFFCCCCCC);
        }

        // sepith totals unchanged
        Map<String,Integer> totals = calculateElementalTotals();
        int tx = leftPos + 348, ty = topPos + 32, off = 0;
        for (var e : totals.entrySet()) {
            gui.drawString(this.font, e.getKey() + ": " + e.getValue(), tx, ty + off, 0xFFFFFF);
            off += 10;
        }

        // locked‐slot X
        int unlocked = this.menu.getOrbmentComponent().getUnlockedSlots();
        for (int i = 0; i < 6; i++) {
            if (i >= unlocked) {
                double ang = 2*Math.PI*i/6 - Math.PI/2;
                int x = (int)(centerX + 40*Math.cos(ang)) - 3;
                int y = (int)(centerY + 40*Math.sin(ang)) - 4;
                gui.drawString(this.font, "X", x, y, 0xFF3333);
            }
        }
    }

    // ─── scrollbar dragging ─────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int trackX = leftPos + ARTS_X + ARTS_W - SCROLL_MARGIN - SCROLL_W;
            int trackY = topPos  + ARTS_Y + SCROLL_MARGIN;
            int trackH = ARTS_H - SCROLL_MARGIN*2;
            if (mx >= trackX && mx <= trackX + SCROLL_W &&
                    my >= trackY && my <= trackY + trackH) {
                draggingThumb = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (draggingThumb && b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int trackY  = topPos + ARTS_Y + SCROLL_MARGIN;
            int trackH  = ARTS_H - SCROLL_MARGIN*2;
            int maxOff  = Math.max(1, artsUnlocked.size() - MAX_ARTS_VISIBLE);
            // map middle‐of‐thumb to offset:
            double p = (my - trackY) / (double)trackH;
            scrollOffset = (int)(p * maxOff + 0.5);
            clampScroll();
            return true;
        }
        return super.mouseDragged(mx, my, b, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        if (draggingThumb && b == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingThumb = false;
            return true;
        }
        return super.mouseReleased(mx, my, b);
    }

    // ─────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollOffset++;
            clampScroll();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollOffset--;
            clampScroll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String formatElem(Map<String,Integer> e) {
        return e.entrySet().stream()
                .map(x -> x.getValue() + x.getKey().substring(0,1).toUpperCase())
                .reduce((a,b)->a+" "+b)
                .orElse("");
    }

    private void drawPanel(GuiGraphics gui, int x, int y, int w, int h, int fill) {
        int border = 0xFF704214, shadow = 0xFF1A0F05;
        gui.fill(x+2, y+2, x+w+2, y+h+2, shadow);
        gui.fill(x,   y,   x+w,   y+h,   fill);
        gui.fill(x,   y,   x+w,   y+1,   border);
        gui.fill(x,   y+h-1, x+w, y+h,   border);
        gui.fill(x,   y,   x+1,   y+h,   border);
        gui.fill(x+w-1, y,   x+w,   y+h,   border);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // no vanilla “Inventory” text
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float pt) {
        gui.fill(0, 0, this.width, this.height, 0xD0101010);
        super.render(gui, mouseX, mouseY, pt);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
