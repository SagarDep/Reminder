package com.elementary.tasks.core.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;
import android.view.Gravity;

import com.elementary.tasks.R;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.ThemeUtil;

public class IconRadioButton extends AppCompatRadioButton {

    private static final String TAG = "IconRadioButton";

    private boolean isDark = false;
    private boolean isCheckable = false;
    private int selectedBg;
    private int icon;

    public IconRadioButton(Context context) {
        super(context);
        init(context, null);
    }

    public IconRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public IconRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        ThemeUtil themeUtil = ThemeUtil.getInstance(context);
        isDark = themeUtil.isDark();
        selectedBg = getResources().getColor(themeUtil.colorAccent());
        if (Module.isMarshmallow()) {
            setTextAppearance(android.R.style.TextAppearance_Small);
        } else {
            setTextAppearance(context, android.R.style.TextAppearance_Small);
        }
        setMaxLines(1);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setButtonDrawable(null);
        setSingleLine(true);
        boolean isChecked = false;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconRadioButton, 0, 0);
            String titleText = "";
            try {
                titleText = a.getString(R.styleable.IconRadioButton_ir_text);
                float textSize = a.getDimension(R.styleable.IconRadioButton_ir_text_size, 16);
                isChecked = a.getBoolean(R.styleable.IconRadioButton_ir_checked, false);
                isCheckable = a.getBoolean(R.styleable.IconRadioButton_ir_checkable, true);
                if (isDark) {
                    icon = a.getResourceId(R.styleable.IconRadioButton_ir_icon_light, 0);
                } else {
                    icon = a.getResourceId(R.styleable.IconRadioButton_ir_icon, 0);
                }
                setTextSize(textSize);
            } catch (Exception e) {
                LogUtil.d(TAG, "There was an error loading attributes.");
            } finally {
                a.recycle();
            }
            setText(titleText);
        }
        if (isCheckable) {
            setChecked(isChecked);
        }
        if (icon != 0) {
            setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(icon), null, null);
        }
        refreshView();
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        if (icon == 0) {
            return;
        }
        this.icon = icon;
        setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(icon), null, null);
    }

    private void refreshView() {
        if (!isEnabled()) {
            setTextColor(getResources().getColor(R.color.material_divider));
            return;
        }
        if (isChecked()) {
            setTextColor(selectedBg);
        } else {
            if (isDark) {
                setTextColor(getResources().getColor(R.color.material_white));
            } else {
                setTextColor(getResources().getColor(R.color.material_grey));
            }
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (!isCheckable) {
            return;
        }
        super.setChecked(checked);
        refreshView();
    }

    @Override
    public void toggle() {
        if (!isCheckable) {
            return;
        }
        if (!isChecked()) {
            setChecked(true);
        }
    }
}
