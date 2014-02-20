package com.licryle.veliby.UI;

import com.licryle.veliby.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

public class WelcomeDialog extends DialogFragment {
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    builder.setView(inflater.inflate(R.layout.dialog_help, null))
    		.setPositiveButton(R.string.welcome_ok,
		    		new DialogInterface.OnClickListener() {
		        	public void onClick(DialogInterface dialog, int id) {
		             dialog.dismiss();
		          }
		        });

    return builder.create();
  }
}
