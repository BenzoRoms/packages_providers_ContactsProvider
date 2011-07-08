/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.providers.contacts;

import static com.android.providers.contacts.util.DbQueryUtils.concatenateClauses;

import com.android.providers.contacts.VoicemailContentProvider.UriData;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.VoicemailContract.Status;

/**
 * Implementation of {@link VoicemailTable.Delegate} for the voicemail status table.
 */
public class VoicemailStatusTable implements VoicemailTable.Delegate {
    private static final ProjectionMap sStatusProjectionMap = new ProjectionMap.Builder()
            .add(Status._ID)
            .add(Status.CONFIGURATION_STATE)
            .add(Status.DATA_CHANNEL_STATE)
            .add(Status.NOTIFICATION_CHANNEL_STATE)
            .add(Status.SETTINGS_URI)
            .add(Status.SOURCE_PACKAGE)
            .add(Status.VOICEMAIL_ACCESS_URI)
            .build();

    private final String mTableName;
    private final Context mContext;
    private final SQLiteOpenHelper mDbHelper;
    private final VoicemailTable.DelegateHelper mDelegateHelper;

    public VoicemailStatusTable(String tableName, Context context, SQLiteOpenHelper dbHelper,
            VoicemailTable.DelegateHelper delegateHelper) {
        mTableName = tableName;
        mContext = context;
        mDbHelper = dbHelper;
        mDelegateHelper = delegateHelper;
    }

    @Override
    public Uri insert(UriData uriData, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues copiedValues = new ContentValues(values);
        mDelegateHelper.checkAndAddSourcePackageIntoValues(uriData, copiedValues);
        long rowId = db.insert(mTableName, null, copiedValues);
        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(uriData.getUri(), rowId);
            mDelegateHelper.notifyChange(newUri, Intent.ACTION_PROVIDER_CHANGED);
            return newUri;
        } else {
            return null;
        }
    }

    @Override
    public int bulkInsert(UriData uriData, ContentValues[] valuesArray) {
        throw new UnsupportedOperationException("bulkInsert is not supported for status table");
    }

    @Override
    public int delete(UriData uriData, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String combinedClause = concatenateClauses(selection, uriData.getWhereClause());
        int count = db.delete(mTableName, combinedClause, selectionArgs);
        if (count > 0) {
            mDelegateHelper.notifyChange(uriData.getUri(), Intent.ACTION_PROVIDER_CHANGED);
        }
        return count;
    }

    @Override
    public Cursor query(UriData uriData, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(mTableName);
        qb.setProjectionMap(sStatusProjectionMap);
        qb.setStrict(true);

        String combinedClause = concatenateClauses(selection, uriData.getWhereClause());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, combinedClause, selectionArgs, null, null, sortOrder);
        if (c != null) {
            c.setNotificationUri(mContext.getContentResolver(), Status.CONTENT_URI);
        }
        return c;
    }

    @Override
    public int update(UriData uriData, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String combinedClause = concatenateClauses(selection, uriData.getWhereClause());
        int count = db.update(mTableName, values, combinedClause, selectionArgs);
        if (count > 0) {
            mDelegateHelper.notifyChange(uriData.getUri(), Intent.ACTION_PROVIDER_CHANGED);
        }
        return count;
    }

    @Override
    public String getType(UriData uriData) {
        if (uriData.hasId()) {
            return Status.ITEM_TYPE;
        } else {
            return Status.DIR_TYPE;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(UriData uriData, String mode) {
        throw new UnsupportedOperationException("File operation is not supported for status table");
    }
}
