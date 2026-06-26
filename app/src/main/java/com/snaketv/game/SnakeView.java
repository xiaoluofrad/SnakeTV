package com.snaketv.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * 游戏渲染View
 * 将 SnakeGame 的数据绘制到屏幕上，适配全屏TV显示
 */
public class SnakeView extends View {

    private final SnakeGame game;
    private final Paint paintBg     = new Paint();
    private final Paint paintGrid   = new Paint();
    private final Paint paintSnakeHead = new Paint();
    private final Paint paintSnakeBody = new Paint();
    private final Paint paintFood   = new Paint();
    private final Paint paintText   = new Paint();
    private final Paint paintOverlay = new Paint();

    private float cellW, cellH;
    private float offsetX, offsetY;

    // 颜色定义
    private static final int COLOR_BG       = Color.parseColor("#1A1A2E");
    private static final int COLOR_GRID      = Color.parseColor("#252545");
    private static final int COLOR_HEAD      = Color.parseColor("#00E676");
    private static final int COLOR_BODY      = Color.parseColor("#00C853");
    private static final int COLOR_FOOD      = Color.parseColor("#FF5722");
    private static final int COLOR_TEXT      = Color.parseColor("#FFFFFF");
    private static final int COLOR_OVERLAY   = Color.parseColor("#CC000000");
    private static final int COLOR_SCORE_BG  = Color.parseColor("#33FFFFFF");

    public SnakeView(Context context, SnakeGame game) {
        super(context);
        this.game = game;
        setFocusable(true);
        setFocusableInTouchMode(false);

        paintBg.setColor(COLOR_BG);
        paintBg.setStyle(Paint.Style.FILL);

        paintGrid.setColor(COLOR_GRID);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(0.5f);

        paintSnakeHead.setColor(COLOR_HEAD);
        paintSnakeHead.setStyle(Paint.Style.FILL);
        paintSnakeHead.setAntiAlias(true);

        paintSnakeBody.setColor(COLOR_BODY);
        paintSnakeBody.setStyle(Paint.Style.FILL);
        paintSnakeBody.setAntiAlias(true);

        paintFood.setColor(COLOR_FOOD);
        paintFood.setStyle(Paint.Style.FILL);
        paintFood.setAntiAlias(true);

        paintText.setColor(COLOR_TEXT);
        paintText.setAntiAlias(true);
        paintText.setTypeface(Typeface.DEFAULT_BOLD);

        paintOverlay.setColor(COLOR_OVERLAY);
        paintOverlay.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalcLayout(w, h);
    }

    private void recalcLayout(int w, int h) {
        // 留出顶部60dp显示分数
        float topBar = dp(60);
        float availW = w;
        float availH = h - topBar;

        cellW = availW / SnakeGame.COLS;
        cellH = availH / SnakeGame.ROWS;

        // 保持正方形格子
        float cell = Math.min(cellW, cellH);
        cellW = cell;
        cellH = cell;

        // 居中对齐
        offsetX = (w - cellW * SnakeGame.COLS) / 2f;
        offsetY = topBar + (availH - cellH * SnakeGame.ROWS) / 2f;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        // 背景
        canvas.drawRect(0, 0, w, h, paintBg);

        // 网格
        for (int c = 0; c <= SnakeGame.COLS; c++) {
            float x = offsetX + c * cellW;
            canvas.drawLine(x, offsetY, x, offsetY + SnakeGame.ROWS * cellH, paintGrid);
        }
        for (int r = 0; r <= SnakeGame.ROWS; r++) {
            float y = offsetY + r * cellH;
            canvas.drawLine(offsetX, y, offsetX + SnakeGame.COLS * cellW, y, paintGrid);
        }

        // 绘制蛇身
        java.util.LinkedList<int[]> snake = game.snake;
        for (int i = snake.size() - 1; i >= 0; i--) {
            int[] seg = snake.get(i);
            Paint p = (i == 0) ? paintSnakeHead : paintSnakeBody;
            float margin = (i == 0) ? 2f : 3f;
            RectF rect = cellRect(seg[0], seg[1], margin);
            float r = (i == 0) ? cellW * 0.35f : cellW * 0.25f;
            canvas.drawRoundRect(rect, r, r, p);
        }

        // 绘制豆豆（圆形）
        float fx = offsetX + game.food[0] * cellW + cellW / 2f;
        float fy = offsetY + game.food[1] * cellH + cellH / 2f;
        float fr = cellW * 0.32f;
        canvas.drawCircle(fx, fy, fr, paintFood);

        // 顶部分数栏
        drawScoreBar(canvas, w);

        // 叠加状态层
        if (game.state == SnakeGame.State.PAUSED) {
            drawOverlay(canvas, w, h, "按 OK 开始游戏", "方向键控制蛇的移动");
        } else if (game.state == SnakeGame.State.GAME_OVER) {
            drawOverlay(canvas, w, h, "游戏结束", "得分: " + game.score + "  按 OK 重新开始");
        }
    }

    private RectF cellRect(int col, int row, float margin) {
        float left   = offsetX + col * cellW + margin;
        float top    = offsetY + row * cellH + margin;
        float right  = offsetX + (col + 1) * cellW - margin;
        float bottom = offsetY + (row + 1) * cellH - margin;
        return new RectF(left, top, right, bottom);
    }

    private void drawScoreBar(Canvas canvas, int w) {
        float barH = dp(56);
        // 分数背景
        Paint scoreBg = new Paint();
        scoreBg.setColor(COLOR_SCORE_BG);
        scoreBg.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, w, barH, scoreBg);

        // 标题
        paintText.setTextSize(dp(22));
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("贪吃小队吃豆豆", dp(24), barH * 0.68f, paintText);

        // 分数
        paintText.setTextSize(dp(26));
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("得分: " + game.score, w - dp(24), barH * 0.68f, paintText);

        // 速度等级
        paintText.setTextSize(dp(18));
        paintText.setTextAlign(Paint.Align.CENTER);
        int level = game.score / 5 + 1;
        canvas.drawText("速度 Lv." + level, w / 2f, barH * 0.68f, paintText);
    }

    private void drawOverlay(Canvas canvas, int w, int h, String line1, String line2) {
        canvas.drawRect(0, 0, w, h, paintOverlay);

        // 提示框
        float boxW = dp(500), boxH = dp(180);
        float boxL = (w - boxW) / 2f, boxT = (h - boxH) / 2f;
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.parseColor("#EE1A1A2E"));
        boxPaint.setStyle(Paint.Style.FILL);
        boxPaint.setAntiAlias(true);
        canvas.drawRoundRect(new RectF(boxL, boxT, boxL + boxW, boxT + boxH),
                dp(20), dp(20), boxPaint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(COLOR_HEAD);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2));
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(new RectF(boxL, boxT, boxL + boxW, boxT + boxH),
                dp(20), dp(20), borderPaint);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(dp(30));
        paintText.setColor(COLOR_HEAD);
        canvas.drawText(line1, w / 2f, boxT + dp(68), paintText);

        paintText.setTextSize(dp(20));
        paintText.setColor(Color.parseColor("#CCFFFFFF"));
        canvas.drawText(line2, w / 2f, boxT + dp(118), paintText);

        // 恢复颜色
        paintText.setColor(COLOR_TEXT);
    }
}
