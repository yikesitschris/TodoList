package com.sargent.mark.todolist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.sargent.mark.todolist.data.Contract;
import com.sargent.mark.todolist.data.DBHelper;

public class MainActivity extends AppCompatActivity implements AddToDoFragment.OnDialogCloseListener, UpdateToDoFragment.OnUpdateDialogCloseListener{

    private RecyclerView rv;
    private FloatingActionButton button;
    private DBHelper helper;
    private Cursor cursor;
    private SQLiteDatabase db;
    ToDoListAdapter adapter;
    private final String TAG = "mainactivity";

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "oncreate called in main activity");

        button = (FloatingActionButton) findViewById(R.id.addToDo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                AddToDoFragment frag = new AddToDoFragment();
                frag.show(fm, "addtodofragment");
            }
        });
        rv = (RecyclerView) findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (db != null) db.close();
        if (cursor != null) cursor.close();
    }

    @Override
    public void closeDialog(int year, int month, int day, String description, String type) {
        addToDo(db, description, formatDate(year, month, day), type);
        cursor = getAllItems(db);
        adapter.swapCursor(cursor);
    }

    @Override
    protected void onStart() {
        super.onStart();

        helper = new DBHelper(this);
        db = helper.getWritableDatabase();
        cursor = getAllItems(db);

        adapter = new ToDoListAdapter(cursor, new ToDoListAdapter.ItemClickListener() {

            @Override
            public void onItemClick(int pos, String description, String duedate, long id, String type, String status) {
                Log.d(TAG, "item click id: " + id);
                String[] dateInfo = duedate.split("-");
                int year = Integer.parseInt(dateInfo[0].replaceAll("\\s",""));
                int month = Integer.parseInt(dateInfo[1].replaceAll("\\s",""));
                int day = Integer.parseInt(dateInfo[2].replaceAll("\\s",""));

                FragmentManager fm = getSupportFragmentManager();

                UpdateToDoFragment frag = UpdateToDoFragment.newInstance(year, month, day, description, id, type);
                frag.show(fm, "updatetodofragment");
            }
        });

        rv.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                Log.d(TAG, "passing id: " + id);
                removeToDo(db, id);
                adapter.swapCursor(getAllItems(db));
            }
        }).attachToRecyclerView(rv);
    }


    public String formatDate(int year, int month, int day) {
        return String.format("%04d-%02d-%02d", year, month + 1, day);
    }

    private Cursor getType(SQLiteDatabase db, String type) {
        String whereClause = "type = '" + type + "'";

        // query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy)
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                whereClause,
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );
    }

    private Cursor getAllItems(SQLiteDatabase db) {
        return db.query(
                Contract.TABLE_TODO.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE
        );
    }

    private long addToDo(SQLiteDatabase db, String description, String duedate, String type) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_TYPE, type);
        return db.insert(Contract.TABLE_TODO.TABLE_NAME, null, cv);
    }

    private boolean removeToDo(SQLiteDatabase db, long id) {
        Log.d(TAG, "deleting id: " + id);
        return db.delete(Contract.TABLE_TODO.TABLE_NAME, Contract.TABLE_TODO._ID + "=" + id, null) > 0;
    }


    private int updateToDo(SQLiteDatabase db, int year, int month, int day, String description, long id, String type){

        String duedate = formatDate(year, month - 1, day);

        ContentValues cv = new ContentValues();
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION, description);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE, duedate);
        cv.put(Contract.TABLE_TODO.COLUMN_NAME_TYPE, type);
        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }

    @Override
    public void closeUpdateDialog(int year, int month, int day, String description, long id, String type) {
        updateToDo(db, year, month, day, description, id, type);
        adapter.swapCursor(getAllItems(db));
    }

    public static int updateTodoStatus(SQLiteDatabase db, long id, boolean isChecked) {
        ContentValues cv = new ContentValues();
        if (isChecked) {
            cv.put(Contract.TABLE_TODO.COLUMN_NAME_STATUS, "c");
        } else {
            cv.put(Contract.TABLE_TODO.COLUMN_NAME_STATUS, "");
        }

        return db.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.all){
            cursor = getAllItems(db);
            adapter.swapCursor(cursor);
        }
        if(item.getItemId() == R.id.home){
            cursor = getType(db, getString(R.string.home));
            adapter.swapCursor(cursor);

        }
        if(item.getItemId() == R.id.work){
            cursor = getType(db, getString(R.string.work));
            adapter.swapCursor(cursor);
        }
        if(item.getItemId() == R.id.school){
            cursor = getType(db, getString(R.string.school));
            adapter.swapCursor(cursor);
        }
        if(item.getItemId() == R.id.play){
            cursor = getType(db, getString(R.string.play));
            adapter.swapCursor(cursor);
        }

        return super.onOptionsItemSelected(item);
    }
}
