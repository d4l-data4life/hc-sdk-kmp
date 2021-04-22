/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */

package care.data4life.sample;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class FilterDialog extends DialogFragment {

    private Button mBtnFilter;
    private Button mBtnClear;
    private TextView mFromTV;
    private TextView mToTV;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyy", Locale.getDefault());
    private FilterListener filterListener;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String fromText = "None";
    private String toText = "None";

    public static FilterDialog newInstance(LocalDate fromDate, LocalDate toDate, FilterListener filterListener) {
        FilterDialog fragment = new FilterDialog();
        fragment.setFromDate(fromDate);
        fragment.setToDate(toDate);
        fragment.setFilterListener(filterListener);
        return fragment;
    }

    private void setFromDate(LocalDate fromDate) {
        if (fromDate != null) {
            this.fromDate = fromDate;
            fromText = dateFormatter.format(fromDate);
        }
    }

    public void setToDate(LocalDate toDate) {
        if (toDate != null) {
            this.toDate = toDate;
            toText = dateFormatter.format(toDate);
        }
    }

    public void setFilterListener(FilterListener filterListener) {
        this.filterListener = filterListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.ThemeOverlay_AppCompat_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_filter, container, false);

        mBtnFilter = view.findViewById(R.id.filterBTN);
        mBtnClear = view.findViewById(R.id.clearBTN);
        mFromTV = view.findViewById(R.id.fromTV);
        mToTV = view.findViewById(R.id.toTV);
        mFromTV.setText(fromText);
        mToTV.setText(toText);
        mFromTV.setOnClickListener(v -> setFromDate());
        mToTV.setOnClickListener(v -> setToDate());
        mBtnClear.setOnClickListener(v -> {
            filterListener.clearFilter();
            dismiss();
        });
        mBtnFilter.setOnClickListener(v -> {
            filterListener.setFilter(fromDate, toDate);
            dismiss();
        });

        return view;
    }

    private void setFromDate() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), (view, year, monthOfYear, dayOfMonth) -> {
            fromDate = LocalDate.of(year, monthOfYear, dayOfMonth);
            mFromTV.setText(dateFormatter.format(fromDate));
        }, fromDate.getYear(), fromDate.getMonthValue(), fromDate.getDayOfMonth());

        datePickerDialog.show();
    }

    private void setToDate() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), (view, year, monthOfYear, dayOfMonth) -> {
            toDate = LocalDate.of(year, monthOfYear, dayOfMonth);
            mToTV.setText(dateFormatter.format(toDate));
        }, toDate.getYear(), toDate.getMonthValue(), toDate.getDayOfMonth());
        datePickerDialog.show();
    }

    public interface FilterListener {
        void clearFilter();

        void setFilter(LocalDate from, LocalDate to);
    }

}
