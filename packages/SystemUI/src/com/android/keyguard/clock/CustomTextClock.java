package com.android.keyguard.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.systemui.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class CustomTextClock extends TextView {

    private String mDescFormat;
    private String[] mHours;
    private final String[] mMinutes;
    private final Resources mResources;
    private final Calendar mTime = Calendar.getInstance(TimeZone.getDefault());
    private TimeZone mTimeZone;

    private boolean h24;
    private int mAccentColor;
    private int hours;
    private int mClockSize = 40;

    private SettingsObserver mSettingsObserver;

    private final BroadcastReceiver mTimeZoneChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                onTimeZoneChanged(TimeZone.getTimeZone(tz));
                onTimeChanged();
            }
        }
    };

    public CustomTextClock(Context context) {
        this(context, null);
    }

    public CustomTextClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CustomTextClock(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();
        mResources = context.getResources();
        h24 = DateFormat.is24HourFormat(getContext());
        if (!h24) mHours = mResources.getStringArray(R.array.type_clock_hours_12);
            else mHours = mResources.getStringArray(R.array.type_clock_hours_24);
        mMinutes = mResources.getStringArray(R.array.type_clock_minutes);
        mAccentColor = mResources.getColor(R.color.accent_device_default_light);
    }

    public void onTimeChanged() {
        h24 = DateFormat.is24HourFormat(getContext());
        mTime.setTimeInMillis(System.currentTimeMillis());
        setContentDescription(DateFormat.format(mDescFormat, mTime));
        if (!h24) {
             mHours = mResources.getStringArray(R.array.type_clock_hours_12);
             hours = mTime.get(Calendar.HOUR) % 12;
        } else {
             mHours = mResources.getStringArray(R.array.type_clock_hours_24);
             hours = mTime.get(Calendar.HOUR_OF_DAY);
        }
        final int minutes = mTime.get(Calendar.MINUTE) % 60;
        SpannedString rawFormat = (SpannedString) mResources.getQuantityText(R.plurals.type_clock_header, hours);
        Annotation[] annotationArr = (Annotation[]) rawFormat.getSpans(0, rawFormat.length(), Annotation.class);
        SpannableString colored = new SpannableString(rawFormat);
        for (Annotation annotation : annotationArr) {
            if ("color".equals(annotation.getValue())) {
                colored.setSpan(new ForegroundColorSpan(mAccentColor),
                        colored.getSpanStart(annotation),
                        colored.getSpanEnd(annotation),
                        Spanned.SPAN_POINT_POINT);
            }
        }
        setText(TextUtils.expandTemplate(colored, new CharSequence[]{mHours[hours], mMinutes[minutes]}));
    }

    public void onTimeZoneChanged(TimeZone timeZone) {
        mTimeZone = timeZone;
        mTime.setTimeZone(timeZone);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Calendar calendar = mTime;
        TimeZone timeZone = mTimeZone;
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        calendar.setTimeZone(timeZone);
        onTimeChanged();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mTimeZoneChangedReceiver, filter);

        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(new Handler());
        }
        mSettingsObserver.observe();
        updateClockSize();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mTimeZoneChangedReceiver);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        refreshLockFont();
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
               	Settings.System.CUSTOM_TEXT_CLOCK_FONTS, 32);
    }

    private void refreshLockFont() {
        final Resources res = getContext().getResources();
        boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int lockClockFont = isPrimary ? getLockClockFont() : 29;

        switch (lockClockFont) {
            case 0:
                setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case 1:
                setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;
            case 2:
                setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case 3:
                setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                break;
            case 4:
                setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                break;
            case 5:
                setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                break;
            case 6:
                setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
                break;
            case 7:
                setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                break;
            case 8:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                break;
            case 9:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                break;
            case 10:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                break;
            case 11:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                break;
            case 12:
                setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                break;
            case 13:
                setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                break;
            case 14:
                setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                break;
            case 15:
                setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
                break;
            case 16:
                setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
                break;
            case 17:
                setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
                break;
            case 18:
                setTypeface(Typeface.create("cursive", Typeface.NORMAL));
                break;
            case 19:
                setTypeface(Typeface.create("cursive", Typeface.BOLD));
                break;
            case 20:
                setTypeface(Typeface.create("casual", Typeface.NORMAL));
                break;
            case 21:
                setTypeface(Typeface.create("serif", Typeface.NORMAL));
                break;
            case 22:
                setTypeface(Typeface.create("serif", Typeface.ITALIC));
                break;
            case 23:
                setTypeface(Typeface.create("serif", Typeface.BOLD));
                break;
            case 24:
                setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
                break;
            case 25:
                setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL));
                break;
            case 26:
                setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL));
                break;
            case 27:
                setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL));
                break;
            case 28:
                setTypeface(Typeface.create("googlesans-sys", Typeface.NORMAL));
                break;
            case 29:
                setTypeface(Typeface.create("neoneon-sys", Typeface.NORMAL));
                break;
            case 30:
                setTypeface(Typeface.create("themeable-sys", Typeface.NORMAL));
                break;
            case 31:
                setTypeface(Typeface.create("samsung-sys", Typeface.NORMAL));
                break;
            case 32:
                setTypeface(Typeface.create("mexcellent-sys", Typeface.NORMAL));
                break;
            case 33:
                setTypeface(Typeface.create("burnstown-sys", Typeface.NORMAL));
                break;
            case 34:
                setTypeface(Typeface.create("dumbledor-sys", Typeface.NORMAL));
                break;
            case 35:
                setTypeface(Typeface.create("phantombold-sys", Typeface.NORMAL));
                break;
            case 36:
                setTypeface(Typeface.create("sourcesanspro-sys", Typeface.NORMAL));
                break;
            case 37:
                setTypeface(Typeface.create("circularstd-sys", Typeface.NORMAL));
                break;
            case 38:
                setTypeface(Typeface.create("oneplusslate-sys", Typeface.NORMAL));
                break;
            case 39:
                setTypeface(Typeface.create("aclonica-sys", Typeface.NORMAL));
                break;
            case 40:
                setTypeface(Typeface.create("amarante-sys", Typeface.NORMAL));
                break;
            case 41:
                setTypeface(Typeface.create("bariol-sys", Typeface.NORMAL));
                break;
            case 42:
                setTypeface(Typeface.create("cagliostro-sys", Typeface.NORMAL));
                break;
            case 43:
                setTypeface(Typeface.create("coolstory-sys", Typeface.NORMAL));
                break;
            case 44:
                setTypeface(Typeface.create("lgsmartgothic-sys", Typeface.NORMAL));
                break;
            case 45:
                setTypeface(Typeface.create("rosemary-sys", Typeface.NORMAL));
                break;
            case 46:
                setTypeface(Typeface.create("sonysketch-sys", Typeface.NORMAL));
                break;
            case 47:
                setTypeface(Typeface.create("surfer-sys", Typeface.NORMAL));
                break;
        }
    }

    public void updateClockSize() {
        mClockSize = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.CUSTOM_TEXT_CLOCK_FONT_SIZE, 40,
                UserHandle.USER_CURRENT);
            setTextSize(mClockSize);
            onTimeChanged();
    }

    protected class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CUSTOM_TEXT_CLOCK_FONT_SIZE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
	    updateClockSize();
        }
    }
}
