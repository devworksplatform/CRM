package crmapp.petsfort.layouts;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import crmapp.petsfort.CartFragmentActivity;
import crmapp.petsfort.R;

public class CartDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a dialog with no title
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate a container layout that will hold the CartFragmentActivity
        View view = inflater.inflate(R.layout.dialog_fragment_container, container, false);

        // Dynamically add the CartFragmentActivity inside this dialog
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CartFragmentActivity())
                    .commit();
        }

        return view;
    }
}
