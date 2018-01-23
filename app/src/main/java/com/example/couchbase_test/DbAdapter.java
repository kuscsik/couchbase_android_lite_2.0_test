package com.example.couchbase_test;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.DocumentChangeListener;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.Log;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.google.gson.Gson;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kuscsik on 1/23/18.
 */

public class DbAdapter {

    private final DatabaseConfiguration config;
    private Replicator replicator;
    private Database database;
    private Map<String, DbChangeNotifier> notifiers = new HashMap<>();

    /**
     * Change this to match your server
     */
    private final String SYNC_SERVER_ADDRESS = "blip://10.10.10.2:4984/test";

    /**
     * Change this to match the bucket name on Couchbase server
     */
    private final String BUCKET_NAME = "testbucket";
    private ListenerToken listenerToken;

    /**
     * Init replicator. Must be called after database is initialized.
     */
    private void initReplicator() {
        URI uri = null;
        try {
            uri = new URI(SYNC_SERVER_ADDRESS);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        ReplicatorConfiguration replicatorConfiguration = new ReplicatorConfiguration(database, uri);
        replicatorConfiguration.setContinuous(true);
        replicatorConfiguration.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);
        replicator = new Replicator(replicatorConfiguration);

        replicator.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(ReplicatorChange change) {
                Log.i("TAG", "Change : "+ change.getStatus());
                //TODO: callbacks on replicator status change.
                // We could provide a feedback to the user, when replication is offline.
            }
        });

        replicator.start();

    }

    DbAdapter(Context context) {
         config = new DatabaseConfiguration(context);

        try {
            Database.setLogLevel(Database.LogDomain.ALL, Database.LogLevel.VERBOSE);
            database = new Database("test", config);

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        initReplicator();
    }

    /**
     * Store POJO object in database
     *
     * @param documentId
     * @param key
     * @param obj
     */
    public boolean saveObjectToDB(String documentId, String key, Object obj) {
        Document doc = database.getDocument(documentId);
        MutableDocument mutableDocument;

        if(doc == null) {
            mutableDocument = new MutableDocument(documentId);
        } else {
            mutableDocument = doc.toMutable();
        }
        Gson gson = new Gson();
        String jsonData = gson.toJson(obj);

        Map<String,Object> result = new Gson().fromJson(jsonData, Map.class);

        mutableDocument.setData(result);

        try {
            database.save(mutableDocument);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }

        return  true;
    }

    public abstract static class DbChangeNotifier {
        abstract void brandUpdated(String brand);
    }

    /**
     * Notify if brand changed
     * @param documentId
     * @param notifier
     */
    public void setBrandChangedNotifier(String documentId, DbChangeNotifier notifier) {
        notifiers.put(documentId, notifier);

        listenerToken = database.addDocumentChangeListener(documentId, new DocumentChangeListener() {
            /**
             * Called when other device modifies the document
             * @param change
             */
            @Override
            public void changed(DocumentChange change) {
                Document doc = database.getDocument(change.getDocumentID());
                DbChangeNotifier cb = notifiers.get(change.getDocumentID());
                if (cb!=null) {
                    cb.brandUpdated(doc.getString("brand"));
                }
            }
        });
    }

    void saveBrand(String documentId, String brand) {

        Document doc = database.getDocument(documentId);
        MutableDocument mutableDocument;

        if(doc == null) {
            mutableDocument = new MutableDocument(documentId);
        } else {
            mutableDocument = doc.toMutable();
        }


        mutableDocument.setValue("brand",  brand);

        try {
            database.save(mutableDocument);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
         }
    }
}
