package com.nevena.absudacity.inventory;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.nevena.absudacity.inventory.data.ProductContract.ProductEntry;

public class ProductCursorAdapter extends CursorAdapter {

    private Context productContext;


    public ProductCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0 /* flags */);
        this.productContext = context;
    }

    // Make new blank list item view. No data is set (or bound) to the views yet
     @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    // Bind product data (in the current row pointed to by cursor) to list item layout
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find individual views that we want to modify in the list item layout
        TextView name = (TextView) view.findViewById(R.id.item_name);
        final TextView quantity = (TextView) view.findViewById(R.id.item_quantity);
        TextView price = (TextView) view.findViewById(R.id.item_price);
        Button sold = (Button) view.findViewById(R.id.sale_button);

        final long id = cursor.getLong(cursor.getColumnIndex(ProductEntry._ID));

        // Find the columns of product attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);

        // Read the product attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        String productQuantity = cursor.getString(quantityColumnIndex);
        String productPrice = cursor.getString(priceColumnIndex);

        // Update the TextViews with the attributes for the current product
        name.setText(productName);
        price.setText(productPrice);
        quantity.setText(productQuantity);

        sold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentQuantity = Integer.parseInt(quantity.getText().toString());
                if (currentQuantity < 1) return;
                currentQuantity--;
                Uri mUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                final ContentValues values = new ContentValues();
                String productQuantity = String.valueOf(currentQuantity);
                values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, productQuantity);
                productContext.getContentResolver().update(mUri, values, null, null);
                quantity.setText(productQuantity);
            }
        });
    }
}