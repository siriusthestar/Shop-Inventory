package com.nevena.absudacity.inventory;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.nevena.absudacity.inventory.data.ProductContract.ProductEntry;

import static com.nevena.absudacity.inventory.data.ProductProvider.LOG_TAG;

// Allows user to insert a new product
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int SELECT_PHOTO_REQ = 0;
    private static final int SEND_MAIL_REQ = 0;
    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private ImageView mImage;
    private Uri mUri;
    private int quantity;
    private Button mIncrease;
    private Button mDecrease;
    private Button mOrder;

    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };
    // OnClickListener for buttons
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.increase:
                    if (mQuantityEditText.getText().toString().equals("")) {
                        /*
                        mIncrease.setEnabled(false);
                        Toast.makeText(EditorActivity.this, getResources().getString(R.string.error_product_quantity_missing),
                                Toast.LENGTH_SHORT).show();
                                */
                        mQuantityEditText.setText("1");
                    } else
                        increaseQuantity();
                    break;
                case R.id.decrease:
                    if (mQuantityEditText.getText().toString().equals("")) {
                        /*
                        mIncrease.setEnabled(false);
                        Toast.makeText(EditorActivity.this, getResources().getString(R.string.error_product_quantity_missing),
                                Toast.LENGTH_SHORT).show();
                                */
                        mQuantityEditText.setText("0");
                        //mDecrease.setEnabled(false);
                    } else
                        decreaseQuantity();
                    break;
                case R.id.action_order:
                    orderTheProduct();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(getString(R.string.editor_activity_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit product"
            setTitle(getString(R.string.editor_activity_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mImage = (ImageView) findViewById(R.id.image);
        mIncrease = (Button) findViewById(R.id.increase);
        mDecrease = (Button) findViewById(R.id.decrease);
        mOrder = (Button) findViewById(R.id.action_order);

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mImage.setOnTouchListener(mTouchListener);
        mIncrease.setOnClickListener(mClickListener);
        mDecrease.setOnClickListener(mClickListener);
        mOrder.setOnClickListener(mClickListener);

        // Picture selector
        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector();
            }
        });
    }

    // returns false if validation fails
    private boolean saveProduct() {
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values
        ContentValues values = new ContentValues();

        if (!TextUtils.isEmpty(nameString)) {
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        } else {
            Toast.makeText(this, getResources().getString(R.string.error_product_name_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        int quantity;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        } else {
            Toast.makeText(this, getResources().getString(R.string.error_product_quantity_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        int price;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        } else {
            Toast.makeText(this, getResources().getString(R.string.error_product_price_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mUri != null) {
            values.put(ProductEntry.COLUMN_PRODUCT_PHOTO, mUri.toString());
        } else {
            Toast.makeText(this, getResources().getString(R.string.error_product_photo_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mCurrentProductUri == null) {
            // This is a new product, so insert a new product into the provider,
            // returning the content URI for the new product
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion
                Toast.makeText(this, getString(R.string.editor_insert_product_fail),
                        Toast.LENGTH_SHORT).show();
                return false;
            } else {
                // Otherwise, the insertion was successful and we can display a toast
                Toast.makeText(this, getString(R.string.editor_insert_product_success),
                        Toast.LENGTH_SHORT).show();
            }
        } else {

            // Otherwise this is an EXISTING product, so update the product with content URI
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_insert_product_fail),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_success),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    //This method is called after invalidateOptionsMenu(), so that the menu can be updated
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (saveProduct()) {
                    // Exit activity
                    finish();
                }
                return true;
            case R.id.action_order:
                orderTheProduct();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // If there are unsaved changes, setup a dialog to warn the user
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the messages
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_fail),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_success),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PHOTO};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,            // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor dbCursor) {
        if (dbCursor == null || dbCursor.getCount() < 1) {
            return;
        }
        // Proceed with moving to the first row of the Cursor and reading data from it
        if (dbCursor.moveToFirst()) {
            int nameColumnIndex = dbCursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = dbCursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = dbCursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int imageColumnIndex = dbCursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO);

            // Extract out the value from the Cursor for the given column index
            String name = dbCursor.getString(nameColumnIndex);
            int quantity = dbCursor.getInt(quantityColumnIndex);
            int price = dbCursor.getInt(priceColumnIndex);
            String image = dbCursor.getString(imageColumnIndex);
            final Uri mImageUri = Uri.parse(image);

            mNameEditText.setText(name);
            mQuantityEditText.setText("" + String.valueOf(quantity));
            mPriceEditText.setText("" + String.valueOf(price));
            mUri = mImageUri;
            mImage.setImageURI(mUri);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
    }

    public void openImageSelector() {
        Intent intent;
        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), SELECT_PHOTO_REQ);
    }

    // Process results (image)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE = 0;
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all
        if (requestCode == SELECT_PHOTO_REQ && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter
            if (resultData != null) {
                mUri = resultData.getData();
                Log.v(LOG_TAG, "Uri: " + mUri.toString());
                mImage.setImageURI(mUri);
            }
        }
    }

    private void increaseQuantity() {
        quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        int maximumIncreaseQuantity = 1000;
        if (quantity < maximumIncreaseQuantity) {
            mProductHasChanged = true;
            quantity++;
            mQuantityEditText.setText(String.valueOf(quantity));
        }
    }

    private void decreaseQuantity() {
        quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        if (quantity > 0) {
            mProductHasChanged = true;
            quantity -= 1;
            mQuantityEditText.setText(String.valueOf(quantity));
        }
    }

    private void orderTheProduct() {
        if (mCurrentProductUri != null) {

            Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                    .setStream(mUri)
                    .getIntent();

            // Provide read access
            shareIntent.setData(mCurrentProductUri);
            shareIntent.setType("message/rfc822");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (Build.VERSION.SDK_INT < 21) {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            } else {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            }

            startActivityForResult(Intent.createChooser(shareIntent, "Share with"), SEND_MAIL_REQ);

        }
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // If there are unsaved changes, setup a dialog to warn the user
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Back" button, close the current activity
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }
}
