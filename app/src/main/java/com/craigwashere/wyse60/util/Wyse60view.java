package com.craigwashere.wyse60.util;

import static android.view.MotionEvent.INVALID_POINTER_ID;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.text.Spannable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.core.view.MotionEventCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class Wyse60view extends View {
    private static final String TAG = "Wyse60";
    private Context mContext;

    enum _mode {
        E_NORMAL, E_ESC, E_SKIP_ONE, E_STATUS_LINE, E_SKIP_DEL, E_FILL_SCREEN,
        E_GOTO_SEGMENT, E_GOTO_ROW_CODE, E_GOTO_COLUMN_CODE, E_GOTO_ROW,
        E_GOTO_COLUMN, E_SET_FIELD_ATTRIBUTE, E_SET_ATTRIBUTE,
        E_GRAPHICS_CHARACTER, E_SET_FEATURES, E_FUNCTION_KEY,
        E_SET_SEGMENT_POSITION, E_SELECT_PAGE, E_CSI_D, E_CSI_E,
        E_ADV_FEATURES, E_LOAD_CHAR,
        E_FIELD_OR_MESSAGE, MESSAGE_UNSHIFTED, MESSAGE_SHIFTED
    }

    private static final short T_NORMAL = 0x30,
            T_BLANK = 0x01,
                T_BLINK = 0x02,
            T_REVERSE = 0x04,
            T_UNDERSCORE = 0x08,
            T_DIM = 0x40,
            T_BOTH = 68,
            T_ALL = 0x7f,
            T_PROTECTED = 256,
            T_GRAPHICS = 512;

    _mode mode = _mode.E_NORMAL;
    int screenWidth = 80, screenHeight = 42, originalWidth, originalHeight;
    int needsReset, needsClearingBuffers, isPrinting;
    int _protected, writeProtection, m_current_attributes;
    int normalAttributes, protectedAttributes = T_REVERSE;
    int protectedPersonality = T_REVERSE;
    int insertMode, graphicsMode, cursorIsHidden, currentPage;
    int changedDimensions, targetColumn, targetRow;
    int custom_display_attributes = 0;
    int bb_count = 0;
    boolean connected, m_display_message = true;

    float m_write_pos_x, m_write_pos_y;
    float m_text_size = 24F, m_text_spacing = 0F;
    float m_font_width = 11.0F, m_font_height = 20.0F;
    int m_char_pos_x, m_char_pos_y;
    boolean ack_mode = false;
    private String drawText = "craig was here";
    Paint mPaintText;
    ConcurrentLinkedQueue<String> message_queue;

    boolean show_cursor = true;

    public void setTextSize(int value)
    {
        m_text_size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) value, getResources().getDisplayMetrics()
        );
        invalidate();
    }

    public void setTextSpacing(float value) {
        m_text_spacing = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) value, getResources().getDisplayMetrics()
        );
        invalidate();
    }


    MainText m_main_text;
    StatusLine m_status_line;
    MessageLine m_message_line;
    public void setText2(String text) {
        Log.d(TAG, "setText2: "+char_to_hex(text));
        if (text.length() > 0) {
            message_queue.add(text);
        }
        onDraw2();
    }

    private void onDraw2() {
        while (!message_queue.isEmpty()) {
            String element = message_queue.remove();
//            Log.d(TAG, "onDraw: " + mode + ' ' + char_to_hex(element));
            for (char c : element.toCharArray()) {
                outputCharacter(0, (char) c);
            }
        }

        invalidate();
    }

/*---------- Scaling code ----------*/
    private ScaleGestureDetector mScaleGestureDetector;

    private float mScaleFactor = 1.0f, mLastTouchX, mLastTouchY, mPosX, mPosY;

    // The ‘active pointer’ is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleGestureDetector.onTouchEvent(ev);

        final int action = ev.getAction();

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
            {
                final int pointerIndex = ev.getActionIndex();
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Remember where we started (for dragging)
                mLastTouchX = x;
                mLastTouchY = y;
                // Save the ID of this pointer (for dragging)
                mActivePointerId = ev.getPointerId(0);
                Log.d(TAG, "onTouchEvent: ACTION_DOWN" + mActivePointerId);
                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                Log.d(TAG, "onTouchEvent: ACTION_MOVE");
                // Find the index of the active pointer and fetch its position
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);

                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Calculate the distance moved
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                mPosX += dx;
                mPosY += dy;

                invalidate();

                // Remember this touch position for the next move event
                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            {
                Log.d(TAG, "onTouchEvent: ACTION_CANCEL");
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            {
                Log.d(TAG, "onTouchEvent: ACTION_POINTER_UP");

                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);

                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,Math.min(mScaleFactor, 10.0f));
            invalidate();
            return true;
        }
    }
/*---------- Scaling code ----------*/

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.translate(mPosX, mPosY);

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        m_main_text.draw_text(canvas, mPaintText);
        m_status_line.draw_text(canvas, mPaintText);

        if (m_display_message)  m_message_line.draw_text(canvas, mPaintText);

        if (show_cursor)    draw_cursor(canvas);
    }


    private void draw_cursor(Canvas canvas)
    {
        mPaintText.setAlpha(200);

        canvas.drawRect(m_write_pos_x, (float) m_write_pos_y,
                m_write_pos_x+m_font_width, m_write_pos_y-m_font_height, mPaintText );
    }

//    private PublishSubject<MyEvent> eventSubject = PublishSubject.create();
//    public void publishEvent(MyEvent event)
//    {
//        Log.d(TAG, "publishEvent: ");
//        eventSubject.onNext(event);
//    }
    String debug_string = "";

    public Wyse60view(Context context)
    {
        super(context);
        Log.d(TAG, "wyse60_TextView: (Context context)");
//        currentBuffer = new ScreenBuffer();

        mPaintText.setColor(Color.GREEN);
    }

    public Wyse60view(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        connected = false;
        Log.d(TAG, "wyse60_TextView: (Context context, AttributeSet attrs)");

        message_queue = new ConcurrentLinkedQueue<String>();

        float text_size = 24F;

        mPaintText = new Paint();
        mPaintText.setColor(Color.GREEN);

        mPaintText.setTextSize(text_size);
        mPaintText.setTypeface(Typeface.MONOSPACE);

        m_write_pos_x = 0F;
        m_write_pos_y = m_font_height;
        m_char_pos_x = 0;
        m_char_pos_y = 0;

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        m_main_text = new MainText(m_text_size);
        m_status_line = new StatusLine(screenHeight, m_text_size);
        m_message_line = new MessageLine(screenHeight+1, m_text_size);
    }

    public Wyse60view(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "wyse60_TextView: (Context context, AttributeSet attrs, int defStyleAttr)");

        mPaintText.setColor(Color.GREEN);
    }

    public void logDecode(String format, Object... args)
    {
        //debug_string += String.format(format, args);
    }

    public void logDecodeFlush() 
    {
       // Log.d(TAG, debug_string);
        debug_string = "";
    }

    public void setProtected(int flag) {
        _protected = flag;
        updateAttributes();
    }

    private void setFeatures(int attributes)
    {
        attributes &= T_ALL;
        protectedPersonality = attributes;
        updateAttributes();
    }

    public void updateAttributes()
    {
        int attributes;

        if (_protected != 0) {
            attributes = normalAttributes | protectedAttributes;
        } else
            attributes = normalAttributes;
    }


    public void setAttributes(int attributes)
    {
        m_current_attributes = attributes;
        m_main_text.change_attribute(attributes);
    }

    private int[] string_to_int_array(String message)
    {
        int message_length = message.length();
        char[] message2 = message.toCharArray();

        int[] return_array = new int[message_length];

        for (int i = 0; i < message_length; i++)
            return_array[i] = (int)message2[i];

        return return_array;
    }

    private String char_to_hex(String message)
    {
        String return_string = new String();
        for (char c: message.toCharArray())
            return_string += (Integer.toHexString(c) + ' ');

        return return_string;
    }
    public void sendUserInput(int pty, String message, int message_length)
    {
        Log.d(TAG, "sendUserInput: ");
        //publishEvent(new MyEvent(message));

        Log.d(TAG, "sendUserInput: message: "+ char_to_hex(message));
        Intent intent = new Intent("send_character");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

    }

    public void setWriteProtection(int flag) {
        writeProtection = flag;
    }

    public int logicalWidth() {
        return 80;
    }

    public int logicalHeight() {
        return 42;
    }

    public void fillScreen(int attributes, char fillChar)
    {
        //hopefully, we can just loop through the chars and set the fillChar and attributes
        logDecode("fillScreen: attributes=%d, fillChar=%c", attributes, fillChar);
    }

    public void clearScreen()
    {
        Log.d(TAG, "clearScreen: ");

        //reset view if it was adjusted
        mPosX = 0;
        mPosY = 0;
        mScaleFactor = 1f;

        m_main_text.clear();
        m_status_line.reset_x_pos();
        m_message_line.reset_x_pos();
    }

    public void _moveScreenBuffer(float x1, float y1, float x2, float y2,
                                  float dx, float dy)
    {   }

    public void _clearScreenBuffer(int x1, int y1, int x2, int y2,
                            short attributes, char fillChar)
    {   }


    public void clearScreenBuffer(int x1, int y1, int x2, int y2,
                           short attributes, char fillChar)
    {
        if (x1 < 0)             x1 = 0;
        if (x2 >= screenWidth)  x2 = screenWidth - 1;
        if (y1 < 0)             y1 = 0;
        if (y2 >= screenHeight) y2 = screenHeight - 1;
        _clearScreenBuffer(x1, y1, x2, y2, attributes, fillChar);
    }


    public void clearExcessBuffers()
    {

    }

    public void moveScreenBuffer(float x1, float y1, float x2, float y2,
                                 float dx, float dy)
    {
        clearExcessBuffers();
        _moveScreenBuffer(x1, y1, x2, y2, dx, dy);
    }

    public void gotoXY(int x, int y)
    {
        logDecode("gotoXY: x=%d, y=%d", x, y);

        m_char_pos_x = x;
        m_char_pos_y = y;

        m_write_pos_x = x * m_font_width;
        m_write_pos_y = y * m_font_height;

        m_main_text.gotoXY(x, y);
    }


    public void gotoXYforce(int x, int y)
    {
        int width = logicalWidth();
        int height = logicalHeight();
        //char buffer[1024];

        /* This function gets called when we do not know where the cursor currently*/
        /* is. So, the safest thing is to use absolute cursor addressing (if       */
        /* available) to force the cursor position. Otherwise, we fall back on     */
        /* relative positioning and keep our fingers crossed.                      */
        if (x >= width) x = width - 1;
        if (x < 0) x = 0;
        if (y >= height) y = height - 1;
        if (y < 0) y = 0;

        logDecode("gotoXYforce: x=%d, y=%d", x, y);
    }


    public void gotoXYscroll(int x, int y)
    {
        int width = logicalWidth();
        int height = logicalHeight();
        //char buffer[ 1024];

        logDecode("gotoXYscroll: x=%d, y=%d\n", x, y);

        gotoXY(x, y);
    }


    public void clearEol()
    {
        logDecode("clearEol");
//        clearExcessBuffers();

        m_main_text.clear_to_end_of_line();
//        for (Iterator<char_text> iterator = chars.iterator(); iterator.hasNext(); )
//        {
//            char_text value = iterator.next();
//            if ((value.m_write_pos_y == m_write_pos_y) && (value.m_write_pos_x >= m_write_pos_x))
//            {
//                value.m_attribute = T_NORMAL;
//                value.character_to_print = ' ';
//            }
//        }
    }

    public void clearEos()
    {
        int width = logicalWidth();
        int height = logicalHeight();
        clearExcessBuffers();

        logDecode("clearEOS");
    }

    public void setPage(int page) {
        logDecode("setPage: page=%d", page);
    }

    public void escape(int pty, char ch)
    {
        logDecode(Character.toString(ch) + ": " +Integer.toHexString(ch));
        logDecodeFlush();

        String buffer = "";
        mode = _mode.E_NORMAL;
        custom_display_attributes = 0;
        switch (ch) {
            case ' ':   /* Reports the terminal identification                           */
                Log.d(TAG, "escape: sendTerminalId()");
                        logDecode("sendTerminalId()");
                        sendUserInput(pty, "60\r", 3);
                        connected = true;
                        break;
            case '!':   /* Writes all unprotected character positions with an attribute  */
                        /* not supported: I don't understand this command */
                        logDecode("NOT SUPPORTED [ 0x1B 0x21");
                        mode = _mode.E_SKIP_ONE;
                        break;
            case '\"':  /* Unlocks the keyboard                                          */
                        /* not supported: keyboard locking */
                        logDecode("unlockKeyboard() /* NOT SUPPORTED */");
                        break;
            case '#':   /* Locks the keyboard                                            */
                        /* not supported: keyboard locking */
                        logDecode("lockKeyboard() /* NOT SUPPORTED */");
                        break;
            case '&':   /* Turns the protect submode on and prevents auto scroll         */
                        logDecode("enableProtected() ");
                        setWriteProtection(1);
                        break;
            case '\'':  /* Turns the protect submode off and allows auto scroll          */
                        logDecode("disableProtected() ");
                        setWriteProtection(0);
                        break;
            case '(':   /*'Turns the write protect submode off                           */
                        logDecode("disableProtected()");
                        setProtected(0);
                        break;
            case ')':   /*'Turns the write protect submode on                            */
                        logDecode("enableProtected()");
                        setProtected(1);
                        break;
            case '*':   /* Clears the screen to nulls; protect submode is turned off     */
                        logDecode("disableProtected() ");
                        logDecode("clearScreen()");
                        setProtected(0);
                        setWriteProtection(0);
                        clearScreen();
                        break;
            case '+':   /* Clears the screen to spaces; protect submode is turned off   */
                        logDecode("disableProtected() ");
                        logDecode("clearScreen()");
                        setProtected(0);
                        setWriteProtection(0);
                        clearScreen();
                        break;
            case ',':   /* Clears screen to protected spaces; protect submode is turned  */
                        /* off                                                           */
                        logDecode("disableProtected() ");
                        logDecode(" ()");
                        setProtected(1);
                        setWriteProtection(0);
                        fillScreen(T_PROTECTED + protectedPersonality, ' ');
                        break;
            case '-':   /* Moves cursor to a specified text segment                      */
                        /* not supported: text segments */
                        logDecode("NOT SUPPORTED [ 0x1B 0x2D ] ");
                        mode = _mode.E_GOTO_SEGMENT;
                        break;
            case '.':   /* Clears all unprotected characters positions with a character  */
                        mode = _mode.E_FILL_SCREEN;
                        break;
            case '/':   {/* Transmits the active text segment number and cursor address   */
                        /* not supported: text segments */
                        logDecode("sendCursorAddress()");
                        buffer.format(" %c%c\r", (m_char_pos_y + 32),
                                (m_char_pos_x + 32));
                        sendUserInput(pty, buffer, 4);
                        }
                        break;
            case '0':   /* Clears all tab settings                                       */
                        /* not supported: tab stops */
                        logDecode("clearAllTabStops() /* NOT SUPPORTED */");
                        break;
            case '1':   /* Sets a tab stop                                               */
                        /* not supported: tab stops */
                        logDecode("setTabStop() /* NOT SUPPORTED */");
                        break;
            case '2':   /* Clears a tab stop                                             */
                        /* not supported: tab stops */
                        logDecode("clearTabStop() /* NOT SUPPORTED */");
                        break;
            case '4':   /* Sends all unprotected characters from the start of row to host*/
                        /* not supported: screen sending */
                        logDecode("sendAllUnprotectedCharactersFromStartOfRow() /* NOT SUPPORTED */");
                        break;
            case '5':   /* Sends all unprotected characters from the start of text to    */
                        /*  host                                                         */
                        /* not supported: screen sending */
                        logDecode("sendAllUnprotectedCharacters() /* NOT SUPPORTED */");
                        break;
            case '6':   /* Sends all characters from the start of row to the host        */
                        /* not supported: screen sending */
                        logDecode("sendAllCharactersFromStartOfRow() /* NOT SUPPORTED */");
                        break;
            case '7':   /* Sends all characters from the start of text to the host       */
                        /* not supported: screen sending */
                        logDecode("sendAllCharacters() /* NOT SUPPORTED */");
                        break;
            case '8':   /* Enters a start of message character (STX)                     */
                        /* not supported: unknown */
                        logDecode("enterSTX() /* NOT SUPPORTED */");
                        break;
            case '9':   /* Enters an end of message character (ETX)                      */
                        /* not supported: unknown */
                        logDecode("enterETX() /* NOT SUPPORTED */");
                        break;
            case ':':   /* Clears all unprotected characters to null                     */
                        logDecode("clearScreen()");
                        clearScreen();
                        break;
            case ';':   /* Clears all unprotected characters to spaces                   */
                        logDecode("clearScreen()");
                        clearScreen();
                        break;
            case '=':   /* Moves cursor to a specified row and column                    */
                        mode = _mode.E_GOTO_ROW_CODE;
                        break;
            case '?':   /* Transmits the cursor address for the active text segment      */
                        logDecode("sendCursorAddress()");
                        buffer.format("%c%c\r", (m_char_pos_y + 32),
                                (m_char_pos_x + 32));
                        sendUserInput(pty, buffer, 3);
                        break;
            case '@':   /* Sends all unprotected characters from start of text to aux    */
                        /* port                                                          */
                        /* not supported: auxiliary port */
                        logDecode("sendAllUnprotectedCharactersToAux() /* NOT SUPPORTED */");
                        break;
            case 'A':   /* Sets the video attributes                                     */
                        logDecode("E_SET_FIELD_ATTRIBUTE");
                        logDecodeFlush();
                        mode = _mode.E_SET_FIELD_ATTRIBUTE;
                        break;
            case 'B':   /* Places the terminal in block mode                             */
                        /* not supported: block mode */
                        logDecode("enableBlockMode() /* NOT SUPPORTED */");
                        break;
            case 'C':   /* Places the terminal in conversation mode                      */
                        /* not supported: block mode */
                        logDecode("enableConversationMode() /* NOT SUPPORTED */");
                        break;
            case 'D':   /* Sets full of half duplex conversation mode                    */
                        /* not supported: block mode */
                        logDecode("enableConversationMode() /* NOT SUPPORTED */ [");
                        mode = _mode.E_SKIP_ONE;
                        break;
            case 'E':   /* Inserts a row of spaces                                       */
                        logDecode("insertLine()");
//                        moveScreenBuffer(0, m_char_pos_y,
//                                logicalWidth() - 1, logicalHeight() - 1,
//                                0, 1);
                        break;
            case 'F':   /* Enters a message in the host message field                    */
                        /* not supported: messages */
                        logDecode("E_STATUS_LINE");
                        logDecodeFlush();
                        logDecode("enterMessage() [");
                        m_status_line.reset_x_pos();
                        mode = _mode.E_STATUS_LINE;
                        break;
            case 'G':   /* Sets a video attributes                                       */
                        mode = _mode.E_SET_ATTRIBUTE;
                        break;
            case 'H':   /* Enters a graphic character at the cursor position             */
                        logDecode("E_GRAPHICS_CHARACTER");
                        logDecodeFlush();
                        mode = _mode.E_GRAPHICS_CHARACTER;
                        break;
            case 'I':   /* Moves cursor left to previous tab stop                        */
                        logDecode("backTab()");
                        //gotoXY((m_char_pos_x - 1) & ~7, current_y);
                        break;
            case 'J':   /* Display previous page                                         */
                        logDecode("displayPreviousPage()");
                        setPage(currentPage - 1);
                        break;
            case 'K':   /* Display next page                                             */
                        logDecode("displayNextPage()");
                        setPage(currentPage + 1);
                        break;
            case 'L':   /* Sends all characters unformatted to auxiliary port            */
                        /* not supported: screen sending  */
                        logDecode("sendAllCharactersToAux() /* NOT SUPPORTED */");
                        break;
            case 'M':   /* Transmit character at cursor position to host                 */
                        /* not supported: screen sending */
                        logDecode("sendCharacter() /* NOT SUPPORTED */");
                        sendUserInput(pty, "\000", 1);
                        break;
            case 'N':   /* Turns no-scroll submode on                                    */
                        /* not supported: scroll mode */
                        logDecode("enableNoScrollMode() /* NOT SUPPORTED */");
                        break;
            case 'O':   /* Turns no-scroll submode off                                   */
                        /* not supported: scroll mode */
                        logDecode("disableNoScrollMode() /* NOT SUPPORTED */");
                        break;
            case 'P':   /* Sends all protected and unprotected characters to the aux port*/
                        /* not supported: screen sending */
                        logDecode("sendAllCharactersToAux() /* NOT SUPPORTED */");
                        break;
            case 'Q':   /* Inserts a character                                           */
                        logDecode("insertCharacter()");
//                        _moveScreenBuffer(m_char_pos_x, m_char_pos_y,
//                                logicalWidth() - 1, m_char_pos_y,
//                                1, 0);
                        break;
            case 'R':   /* Deletes a row                                                 */
                        logDecode("deleteLine()");
//                        moveScreenBuffer(0, m_char_pos_y + 1,
//                                logicalWidth() - 1, logicalHeight() - 1,
//                                0, 1);
                        break;
            case 'S':   /* Sends a message unprotected                                   */
                        /* not supported: messages */
                        logDecode("sendMessage() /* NOT SUPPORTED */");
                        break;
            case 'T':   /* Erases all characters                                         */
                        logDecode("clearToEndOfLine()");
                        clearEol();
                        break;
            case 'U':   /* Turns the monitor submode on                                  */
                        /* not supported: monitor mode */
                        logDecode("enableMonitorMode() /* NOT SUPPORTED */");
                        break;
            case 'V':   /* Sets a protected column                                       */
                        displayCurrentScreenBuffer();
                        break;
            case 'W':   /* Deletes a character                                           */
                        logDecode("deleteCharacter()");
//                        _moveScreenBuffer(m_char_pos_x + 1, m_char_pos_y,
//                                logicalWidth() - 1, m_char_pos_y,
//                                -1, 0);
                        break;
            case 'X':   /* Turns the monitor submode off                                 */
                        /* not supported: monitor mode */
                        logDecode("disableMonitorMode() /* NOT SUPPORTED */");
                        break;
            case 'Y':   /* Erases all characters to the end of the active text segment   */
                        /* not supported: text segments */
                        logDecode("clearToEndOfSegment() /* NOT SUPPORTED */");
                        clearEos();
                        break;
            case 'Z':   /* Program function key sequence                                 */
                        mode = _mode.E_FUNCTION_KEY;
                        break;
            case ']':   /* Activates text segment zero                                   */
                        /* not supported: text segments */
                        logDecode("activateSegment(0) /* NOT SUPPORTED */");
                        break;
            case '^':   /* Select normal or reverse display                              */
                        /* not supported: inverting the entire screen */
                        logDecode("invertScreen() /* NOT SUPPORTED */ [");
                        mode = _mode.E_SKIP_ONE;
                        break;
            case '`':   /* Sets the screen features                                      */
                        mode = _mode.E_SET_FEATURES;
                        break;
            case 'a':   /* Moves the cursor to a specified row and column                */
                        mode = _mode.E_GOTO_ROW;
                        targetColumn = 0;
                        targetRow = 0;
                        break;
            case 'b':   /* Transmits the cursor address to the host                      */
                        logDecode("sendCursorAddress()");
                        buffer.format("%dR%dC", m_char_pos_y + 1, m_char_pos_x + 1);
                        sendUserInput(pty, buffer, buffer.length());
                        break;
            case 'c':   /* Set advanced parameters                                       */
                        /* not supported: advanced parameters */
                        logDecode("setAdvancedParameters() /* NOT SUPPORTED */ [");
                        mode = _mode.E_SKIP_ONE;
                        break;
            case 'd':   /* Line wrap mode, transparent printing, ...                     */
                        mode = _mode.E_CSI_D;
                        break;
            case 'e':   /* Set communication mode                                        */
                        /* not supported: communication modes */
                        mode = _mode.E_CSI_E;
                        break;
            case 'i':   /* Moves the cursor to the next tab stop on the right            */
                        logDecode("tab()");
                        //gotoXY((m_char_pos_x + 8) & ~7, current_y);
                        break;
            case 'j':   /* Moves cursor up one row and scrolls if in top row             */
                        logDecode("moveUpAndScroll()");
                        gotoXYscroll(m_char_pos_x, m_char_pos_y + 1);
                        break;
            case 'k':   /* Turns local edit submode on                                   */
                        /* not supported: local edit mode */
                        logDecode("enableLocalEditMode() /* NOT SUPPORTED */");
                        break;
            case 'l':   /* Turns duplex edit submode on                                  */
                        /* not supported: local edit mode */
                        logDecode("enableDuplexEditMode() /* NOT SUPPORTED */");
                        break;
            case 'p':   /* Sends all characters unformatted to auxiliary port            */
                        /* not supported: auxiliary port */
                        logDecode("sendAllCharactersToAux() /* NOT SUPPORTED */");
                        break;
            case 'q':   /* Turns the insert submode on                                   */
                        logDecode("enableInsertMode()");
                        //if (enter_insert_mode && strcmp(enter_insert_mode, "@"))
                        //    putCapability(enter_insert_mode);
                        insertMode = 1;
                        break;
            case 'r':   /* Turns the insert submode off                                  */
                        logDecode("disableInsertMode()");
                        insertMode = 0;
                        break;
            case 's':   /* Sends a message                                               */
                        /* not supported: messages */
                        logDecode("sendMessage() /* NOT SUPPORTED */");
                        break;
            case 't':   /* Erases from cursor position to the end of the row             */
                        logDecode("clearToEndOfLine()");
                        clearEol();
                        break;
            case 'u':   /* Turns the monitor submode off                                 */
                        /* not supported: monitor mode */
                        logDecode("disableMonitorMode() /* NOT SUPPORTED */");
                        break;
            case 'w':   /* Divide memory into pages; or select page to display           */
                        mode = _mode.E_SELECT_PAGE;
                        break;
            case 'x':   /* Changes the screen display format                             */
                        /* not supported: text segments */
                        mode = _mode.E_SET_SEGMENT_POSITION;
                        break;
            case 'y':   /* Erases all characters from the cursor to end of text segment  */
                        /* not supported: text segments */
                        logDecode("clearToEndOfSegment() /* NOT SUPPORTED */");
                        clearEos();
                        break;
            case 'z':   /* Enters message into key label field                           */
                        logDecode("setKeyLabel() /* NOT SUPPORTED */ [");
                        mode = _mode.E_FIELD_OR_MESSAGE;
                        break;
            case '{':   /* Moves cursor to home position of text segment                 */
                        /* not supported: text segments */
                        logDecode("home()");
                        gotoXY(0, 0);
                        break;
            case '}':   /* Activates text segment 1                                      */
                        /* not supported: text segments */
                        logDecode("activateSegment(0) /* NOT SUPPORTED */");
                        break;
            case '~':   /* Select personality                                            */
                        /* not supported: personalities */
                        logDecode("setPersonality() /* NOT SUPPORTED */ [");
                        mode = _mode.E_SKIP_ONE;
                        break;
        }

        if (mode == _mode.E_NORMAL)
            logDecodeFlush();
    }

    public void displayCurrentScreenBuffer() {
        logDecode("displayCurrentScreenBuffer()");
    }

    public void normal(int pty, char ch)
    {
        switch (ch)
        {
            case 0x00:  /* 0x00  NUL: No action                                             */
                        logDecode("nul() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x01:  /* 0x01  SOH: No action                                             */
                        logDecode("soh() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x02:  /* 0x02  STX: No action                                             */
                        if (mode == _mode.E_GRAPHICS_CHARACTER)
                        {
                            putGraphics(ch); /* doesn't actually output anything */
                            mode = _mode.E_NORMAL;
                        }
                        else
                        {
                            logDecode("stx() /* no action */");
                            logDecodeFlush();
                        }
                        break;
            case 0x03:  /* 0x03  ETX: No action                                             */
                        if (mode == _mode.E_GRAPHICS_CHARACTER)
                        {
                            putGraphics(ch); /* doesn't actually output anything */
                            mode = _mode.E_NORMAL;
                        }
                        else
                        {
                            logDecode("etx() /* no action */");
                            logDecodeFlush();
                        }
                        break;
            case 0x04:  /* 0x04  EOT: No action                                             */
                        logDecode("eot() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x05:  /* 0x05  ENQ: Returns ACK, if not busy                              */
                        logDecode("enq()");
                        if ((connected) && (ack_mode))
                        {
                            char cfgIdentifier = 0x06;
                            sendUserInput(pty, Character.toString((cfgIdentifier)), 1);
                        }
                        logDecodeFlush();
                        break;
            case 0x06:  /* 0x06  ACK: No action                                             */
                        logDecode("ack() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x07:  /* 0x07  BEL: Sound beeper                                          */
                        logDecode("bell()");
                        logDecodeFlush();
                        break;
            case 0x08:  /* 0x08 BS:  Move cursor to the left                               */
                        int x = m_char_pos_x - 1, y = m_char_pos_y;
                        if (x < 0)
                        {
                            x = logicalWidth() - 1;

                            if (--y < 0)
                                y = 0;
                        }
                        logDecode("moveLeft()");
                        gotoXY(x, y);
                        logDecodeFlush();
                        break;
            case 0x09:  /* 0x09  HT:  Move to next tab position on the right                */
                        logDecode("tab()");
                        gotoXY((m_char_pos_x + 8) & ~7, m_char_pos_y);
                        logDecodeFlush();
                        break;
            case 0x0a:  /* 0x0A  LF:  Moves cursor down                                     */
                        logDecode("moveDown() %d, %d", m_char_pos_x, m_char_pos_y);
                        logDecodeFlush();
                        gotoXYscroll(m_char_pos_x, m_char_pos_y + 1);
                        break;
            case 0x0b:  /* 0x0B  VT:  Moves cursor up                                       */
                        logDecode("moveUp()");
                        gotoXY(m_char_pos_x, (m_char_pos_y - 1 + logicalHeight()) % logicalHeight());
                        logDecodeFlush();
                        break;
            case 0x0c:  /* 0X0C  FF:  Moves cursor to the right                             */
                        logDecode("moveRight()");
                        gotoXY(m_char_pos_x + 1, m_char_pos_y);
                        logDecodeFlush();
                        break;
            case 0x0d:  /* 0x0D  CR:  Moves cursor to column one                            */
                        logDecode("return()");
                        gotoXY(0, m_char_pos_y);
                        logDecodeFlush();
                        break;
            case 0x0e:  /* 0x0E  SO:  Unlocks the keyboard                                  */
                        logDecode("so() /* NOT SUPPORTED */");
                        logDecodeFlush();
                        break;
            case 0x0f:  /* 0x0F  SI:  Locks the keyboard                                    */
                        logDecode("si() /* NOT SUPPORTED */");
                        logDecodeFlush();
                        break;
            case 0x10:  /* 0x10  DLE: No action                                             */
                        logDecode("dle() /* NOT SUPPORTED */");
                        logDecodeFlush();
                        break;
            case 0x11:  /* 0x11  XON: Enables the transmitter                               */
                        logDecode("xon() /* NOT SUPPORTED */");
                        logDecodeFlush();
                        break;
            case 0x12:  /* 0x12  DC2: Turns on auxiliary print                              */
                        logDecode("setPrinting(AUXILIARY);");
                        // isPrinting = P_AUXILIARY;
                        logDecodeFlush();
                        break;
            case 0x13:  /* 0x13  XOFF:Stops transmission to host                            */
                        logDecode("xoff() /* NOT SUPPORTED */");
                        logDecodeFlush();
                        break;
            case 0x14:  /* 0x14  DC4: Turns off auxiliary print                             */
                        logDecode("setPrinting(OFF);");
                        //isPrinting = P_OFF;
                        //  flushPrinter();
                        logDecodeFlush();
                        break;
            case 0x15:  /* 0x15  NAK: No action                                             */
                        logDecode("nak() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x16:  /* 0x16  SYN: No action                                             */
                        logDecode("syn() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x17:  /* 0x17  ETB: No action                                             */
                        logDecode("etb() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x18:  /* 0x18  CAN: No action                                             */
                        logDecode("can() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x19:  /* 0x19  EM:  No action                                             */
                        logDecode("em() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x1a:  /* 0x1A  SUB: Clears all unprotected characters                     */
                        logDecode("clearScreen()");
                        clearScreen();
                        logDecodeFlush();
                        break;
            case 0x1b:  /* 0x1B  ESC: Initiates an escape sequence                          */
                        logDecode("Enable Escape Mode: ");
                        //logDecodeFlush();
                        mode = _mode.E_ESC;
                        break;
            case 0x1c:  /* 0x1C  FS:  No action                                             */
                        logDecode("fs() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x1d:  /* 0x1D  GS:  No action                                             */
                        logDecode("gs() /* no action */");
                        logDecodeFlush();
                        break;
            case 0x1e:  /* 0x1E  RS:  Moves cursor to home position                         */
                        logDecode("home()");
                        gotoXY(0, 0);
                        logDecodeFlush();
                        break;
            case 0x1f:  /* 0x1F  US:  Moves cursor down one row to column one               */
                        logDecode("moveDown() ");
                        logDecode("return()");
                        gotoXYscroll(0, m_char_pos_y + 1);
                        logDecodeFlush();
                        break;
            case 0x7f:  /* 0x7F  DEL: Delete character                                      */
                        logDecode("del() /* no action */");
                        logDecodeFlush();
                        break;
            default:
                String graphicsCharacters = "┬└┌┐├┘│█┼┤─▓═┴║▒";
//                Log.d(TAG, "normal: " + ch);

                char replacing_char = 0;
                try 
                {
                    replacing_char = (graphicsMode == 0)? ch : graphicsCharacters.charAt((int)(ch-'0'));
                }
                catch (java.lang.StringIndexOutOfBoundsException e)
                {
                    Log.d(TAG, "normal: char out of range: " + ch);
                }
                finally
                {
                    m_main_text.add_char(replacing_char);
                    m_char_pos_x++;
                    m_write_pos_x += m_font_width;
                }
        }
    }
    public void putGraphics ( char ch)
    {
    }

    public void outputCharacter (int pty, char ch)
    {
        switch (mode) {
            case E_GRAPHICS_CHARACTER:
                switch (ch)
                {
                    case 0x03:  logDecode("Graphics mode off");
                        graphicsMode = 0;
                                break;
                    case 0x02:	logDecode("Graphics mode on");
                        graphicsMode = 1;
                                break;
                    default:	normal(pty, enterGraphicsCharacter(ch));	break;
                }
                mode = _mode.E_NORMAL;

                logDecodeFlush();
                break;
            case E_NORMAL:      normal(pty, ch);
                                break;
            case E_ESC:         escape(pty, ch);
                                break;
            case E_SKIP_ONE:    mode = _mode.E_NORMAL;
                                logDecode(" 0x" + Integer.toHexString(ch) + " ]");
                                break;
            case E_STATUS_LINE:   if (ch == '\r')
                                {
                                    logDecode(" ]");
                                    logDecodeFlush();
                                    mode = _mode.E_NORMAL;
                                }
                                else
                                {
                                    logDecode(" " + Integer.toHexString(ch));
                                    m_status_line.add_char(ch);
                                }
                                break;
            case E_SKIP_DEL:    if (ch == 0X7F || ch == '\r')
                                {
                                    logDecode(" ]");
                                    logDecodeFlush();
                                    mode = _mode.E_NORMAL;
                                }
                                else
                                    logDecode(" " + Integer.toHexString(ch));
                                break;
            case E_FILL_SCREEN: logDecode("fillScreen(0x" + Integer.toHexString(ch));
                                fillScreen(T_NORMAL, ch);
                                break;
            case E_GOTO_SEGMENT:        /* not supported: text segments */
                                mode = _mode.E_GOTO_ROW_CODE;
                                break;
            case E_GOTO_ROW_CODE:   targetRow = (((int) ch) & 0xFF) - 32;
                                    //Log.d(TAG, "outputCharacter: row: " + targetRow);
                                    mode = _mode.E_GOTO_COLUMN_CODE;
                                    break;
            case E_GOTO_COLUMN_CODE:    //Log.d(TAG, "outputCharacter: col: " + ((((int) ch) & 0xFF) - 32));
                                        mode = _mode.E_NORMAL;
                                        gotoXY((((int) ch) & 0xFF) - 32, targetRow);
                                        logDecodeFlush();
                                        break;
            case E_GOTO_ROW:    if (ch == 'R')
                                    mode = _mode.E_GOTO_COLUMN;
                                else
                                    targetRow = 10 * targetRow + (((int) (ch - '0')) & 0xFF);
                                break;
            case E_GOTO_COLUMN: if (ch == 'C')
                                {
                                    //logDecode("gotoXY(%d,%d)", targetColumn - 1, targetRow - 1);
                                    gotoXY(targetColumn - 1, targetRow - 1);
                                    mode = _mode.E_NORMAL;
                                    logDecodeFlush();
                                } else
                                    targetColumn = 10 * targetColumn + (((int) (ch - '0')) & 0xFF);
                                break;
            case E_SET_FIELD_ATTRIBUTE: if (ch != '0')
                                        {
                                            /* not supported: attributes for non-display areas */
                                            logDecode("NOT SUPPORTED [ 0x1B 0x41 0x02", ch);
                                            mode = _mode.E_SKIP_ONE;
                                        } else
                                            mode = _mode.E_SET_ATTRIBUTE;
                                        break;
            case E_SET_ATTRIBUTE:   logDecode("setAttribute(%s%s%s%s%s%s)",
                                            ((ch & T_ALL) == T_NORMAL)          ? " NORMAL"     : "",
                                            (((ch & T_ALL) & T_REVERSE) != 0)   ? " REVERSE"    : "",
                                            (((ch & T_ALL) & T_DIM) != 0)       ? " DIM"        : "",
                                            (((ch & T_ALL) & T_UNDERSCORE) != 0)? " UNDERSCORE" : "",
                                            (((ch & T_ALL) & T_BLINK) != 0)     ? " BLINK"      : "",
                                            (((ch & T_ALL) & T_BLANK) != 0)     ? " BLANK"      : "");
                                    setAttributes(ch);
                                    custom_display_attributes = 1;
                                    mode = _mode.E_NORMAL;
                                    logDecodeFlush();
                                    break;
            case E_SET_FEATURES:
                switch (ch)
                {
                    case '0':   /* Cursor display off                                          */
                                showCursor(0);
                        Log.d(TAG, "outputCharacter: hideCursor()");
                                logDecode("hideCursor()");
                                break;
                    case '1':   /* Cursor display on                                           */
                    case '2':   /* Steady block cursor                                         */
                    case '5':   /* Blinking block cursor                                       */
                                showCursor(1);
                        Log.d(TAG, "outputCharacter: showCursor()");
                                logDecode("showCursor()");
                                break;
                    case '3':   /* Blinking line cursor                                        */
                    case '4':   /* Steady line cursor                                          */
                                showCursor(1);
                                logDecode("dimCursor()");
                                break;
                    case '6':   /* Reverse protected character                                 */
                                setFeatures(T_REVERSE);
                                logDecode("reverseProtectedCharacters()");
                                break;
                    case '7':   /* Dim protected character                                     */
                                setFeatures(T_DIM);
                                logDecode("dimProtectedCharacters()");
                                break;
                    case '8':   /* Screen display off                                          */
                    case '9':   /* Screen display on                                           */
                                /* not supported: disabling screen display */
                                logDecode("NOT SUPPORTED [ 0x1B 0x60 0x" + Integer.toHexString(ch)+ " ]");
                                break;
                    case ':':       /* 80 column mode                                              */
                                requestNewGeometry(pty, 80, screenHeight);
                    case ';':       /* 132 column mode                                             */
                                requestNewGeometry(pty, 132, screenHeight);
                                break;
                    case '<': /* Smooth scroll at one row per second                         */
                    case '=': /* Smooth scroll at two rows per second                        */
                    case '>': /* Smooth scroll at four rows per second                       */
                    case '?': /* Smooth scroll at eight rows per second                      */
                    case '@': /* Jump scroll                                                 */
                                /* not supported: selecting scroll speed */
                                logDecode("NOT SUPPORTED [ 0x1B 0x60 0x" + Integer.toHexString(ch)+ " ]");
                                break;
                    case 'A':       /* Normal protected character                                  */
                                setFeatures(T_NORMAL);
                                logDecode("normalProtectedCharacters()");
                                break;
                    default:    logDecode("UNKNOWN FEATURE [ 0x1B 0x60 0x" + Integer.toHexString(ch)+ " ]");
                                break;
                }
                mode = _mode.E_NORMAL;
                logDecodeFlush();
                break;
            case E_FUNCTION_KEY:    logDecode("NOT SUPPORTED [ 0x1B 0x5A 0x" + Integer.toHexString(ch)+ " ]");
                                    if (ch == '~')
                                    {
                                        /* not supported: programming function keys */
                                        mode = _mode.E_SKIP_ONE;
                                    } else {
                                        /* not supported: programming function keys */
                                        mode = _mode.E_SKIP_DEL;
                                    }
                                    break;
            case E_SET_SEGMENT_POSITION:    logDecode("NOT SUPPORTED [ 0x1B 0x78 0x" + Integer.toHexString(ch)+ " ]");
                                            if (ch == '0')
                                            {
                                                /* not supported: text segments */
                                                logDecode(" ]");
                                                mode = _mode.E_NORMAL;
                                                logDecodeFlush();
                                            }
                                            else
                                            {
                                                /* not supported: text segments */
                                                mode = _mode.E_SKIP_ONE;
                                            }
                                            break;
            case E_SELECT_PAGE:
                switch (ch) {
                    case 'G':   /* Page size equals number of data lines                       */
                    case 'H':   /* Page size is twice the number of data lines                 */
                    case 'J':   /* 1st page is number of data lines,2nd page is remaining lines*/
                                /* not supported: splitting memory */
                                logDecode("NOT SUPPORTED [ 0x1B 0x77 0x" + Integer.toHexString(ch)+ " ]");
                                break;
                    case 'B':   /* Display previous page                                       */
                                logDecode("displayPreviousPage()");
                                setPage(currentPage - 1);
                                break;
                    case 'C':   /* Display next page                                           */
                                logDecode("displayNextPage()");
                                setPage(currentPage + 1);
                                break;
                    case '0':   /* Display page 0                                              */
                                logDecode("displayPage(0)");
                                setPage(0);
                                break;
                    case '1':   /* Display page 1                                              */
                                logDecode("displayPage(1)");
                                setPage(1);
                                break;
                    case '2':   /* Display page 2                                              */
                                /* not supported: page 2 */
                                logDecode("NOT SUPPORTED [ 0x1B 0x77 0x32 ]");
                                setPage(2);
                                break;
                }
                mode = _mode.E_NORMAL;
                logDecodeFlush();
                break;
            case E_CSI_D:   if (ch == '#')
                            {
                                logDecode("setPrinting(TRANSPARENT);");
                                //isPrinting = P_TRANSPARENT;
                            }
                            else{
                                    logDecode("setMode(0x" + Integer.toHexString(ch)+ " /* NOT SUPPORTED */");
                            }
                            mode = _mode.E_NORMAL;
                            logDecodeFlush();
                            break;
            case E_CSI_E:   int newHeight = 42;
                            switch (ch)
                            {
                                case '(':   /* Display 24 data lines                                       */
                                            newHeight = 24;
                                            break;
                                case ')':   /* Display 25 data lines                                       */
                                            newHeight = 25;
                                            break;
                                case '*':   /* Display 42 data lines                                       */
                                            newHeight = 42;
                                            break;
                                case '+':   /* Display 43 data lines                                       */
                                            newHeight = 43;
                                            break;

                    /*0X36*/    case '6':   ack_mode = false;   break;
                    /*0x37*/    case '7':   ack_mode = true;    break;
                                default:    logDecode("setCommunicationMode(0x" + Integer.toHexString(ch)+ " /* NOT SUPPORTED */");
                                            break;
                            }
                            requestNewGeometry(pty, screenWidth, newHeight);
                            mode = _mode.E_NORMAL;
                            logDecodeFlush();
                            break;
            case E_ADV_FEATURES:    switch (ch)
                                    {
                                        case 'A':   logDecode("Define and load character");
                                                    mode = _mode.E_LOAD_CHAR;
                                    }
                                break;
            case E_LOAD_CHAR:   if ((ch == 0X121) || (bb_count == 32))
                                {
                                    logDecode(" ]");
                                    logDecodeFlush();
                                    mode = _mode.E_NORMAL;
                                }
                                else {
                                    logDecode(" " + Integer.toHexString(ch));
                                    bb_count++;
                                }
                                break;
            case E_FIELD_OR_MESSAGE: switch (ch)
                                     {
                            /*0x28*/    case '(':   m_message_line.reset_x_pos();
                                                    mode = _mode.MESSAGE_UNSHIFTED;
                                                    break;
                            /*0x29*/    case ')':   m_message_line.reset_x_pos();
                                                    mode = _mode.MESSAGE_SHIFTED;
                                                    break;
                            /*0x50*/    case 'P':   m_display_message = true;
                                                    mode = _mode.E_NORMAL;
                                                    break;
                            /*DEL */    case 0x7f:  m_display_message = false;
                                                    mode = _mode.E_NORMAL;
                                                    break;
                                        default: mode = _mode.E_FUNCTION_KEY;
                                     }
                                     break;
            case MESSAGE_UNSHIFTED: if (ch == '\r')
                                    {
                                        m_message_line.solidify_string();
                                        mode = _mode.E_NORMAL;
                                        break;
                                    }
                                    else
                                    {
                                        m_message_line.insert_char(ch);
                                    }
        }
    }

    public void requestNewGeometry(int pty, int width, int height)
    {
        logDecode("requestNewGeometry: " + width + ", " + height); logDecodeFlush();

        Log.d(TAG, "requestNewGeometry: " + width + ", " + height);
        screenHeight = height;
        screenWidth = width;

        m_status_line.move_status_line(screenHeight);
    }


    public void showCursor(int flag)
    {
        logDecode("showCursor: %d\n", flag);
        show_cursor = flag != 0;
    }


/*  At one time, I had flashing text. I think this was how
            startTime = Long.valueOf(System.currentTimeMillis());
 */
//  Handler handler;
//  long startTime, currentTime, finishedTime = 0L;
//            currentTime = startTime;
//
//            color = colors[0];
//            handler = new Handler();
//
//            handler.postDelayed(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    int duration = 22000 / 4;
//                    currentTime = Long.valueOf(System.currentTimeMillis());
//                    finishedTime = Long.valueOf(currentTime) - Long.valueOf(startTime);
//
//                    if (color == colors[0])
//                        color = colors[1];
//                    else
//                        color = colors[0];
//
//                    SpannableStringBuilder ssb = new SpannableStringBuilder();
//                    for (int i = 0; i <= currentBuffer.attribute_index; i++)
//                    {
//                        int start = currentBuffer.attributes.get(i).startY * currentBuffer.maximumWidth + currentBuffer.attributes.get(i).startX;
//                        Log.d(TAG, "run: start="+start);
//                        ssb.append(new String(currentBuffer.lineBuffer));
//                        ssb.setSpan(new ForegroundColorSpan(color), start, start+currentBuffer.attributes.get(i).effect_length, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//                    }
//                    setText(ssb, BufferType.SPANNABLE);
//                    handler.postDelayed(this, 500);
//                }
//            }, 10);
//*/
//    }

    public char enterGraphicsCharacter(char key)
    {
        char output_char;
        switch (key)
        {
            case '0':   output_char = '┬';  break; /*xc2 = ┬ */
            case '1':   output_char = '└';  break; /*xc0  = └ */
            case '2':   output_char = '┌';  break; /*\xda  = ┌ */
            case '3':   output_char = '┐';  break; /* \xbf = ┐ */
            case '4':   output_char = '├';  break; /* \xc3 = ├ */
            case '5':   output_char = '┘';  break; /* \xd9 = ┘ */
            case '6':   output_char = '│';  break; /* \xb3 = │ */
            case '7':   output_char = '█';  break; /* '\333' \xdb = █ */
            case '8':   output_char = '┼';  break; /* \xc5 = ┼ */
            case '9':   output_char = '┤';  break; /* \xb4 = ┤ */
            case ':':   output_char = '─';  break; /* \xc4 = ─ */
            case ';':   output_char = '▓';  break; /* \xb2 = ▓ */
            case '<':   output_char = '═';  break; /* \xcd = ═'\315' */
            case '=':   output_char = '┴';  break; /* \xc1 = ┴ */
            case '>':   output_char = '║';  break; /* \xba = ║ */
            case '?':   output_char = '▒';  break; /* \xb1 = ▒ */
            default:    output_char = ' ';  break;
        }

        return output_char;
    }
}
