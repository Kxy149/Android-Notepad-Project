/*
 * Copyright (C) 2007 The Android Open Source Project
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
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.content.ContentValues;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2
            NotePad.Notes.COLUMN_NAME_CATEGORY, // 3
            NotePad.Notes.COLUMN_NAME_PINNED // 4
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFIED = 2;

    private SimpleCursorAdapter mAdapter;
    private SearchView mSearchView;
    private String mCurrentFilter;
    private String mCurrentCategoryFilter;
    private TextView mSearchResultCount;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notes_list);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        final ListView listView = getListView();
        listView.setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = queryNotes(null);

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_CATEGORY,
                NotePad.Notes.COLUMN_NAME_PINNED
        };

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = { R.id.note_title, R.id.note_timestamp, R.id.note_category, R.id.note_pinned };

        // Creates the backing adapter for the ListView.
        SimpleCursorAdapter adapter
            = new SimpleCursorAdapter(
                      this,                             // The Context for the ListView
                      R.layout.noteslist_item,          // Points to the XML for a list item
                      cursor,                           // The cursor to get items from
                      dataColumns,
                      viewIDs
              );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        adapter.setViewBinder((view, cursor1, columnIndex) -> {
            if (view.getId() == R.id.note_timestamp) {
                long time = cursor1.getLong(COLUMN_INDEX_MODIFIED);
                // Format timestamp as full date and time
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String formattedTime = sdf.format(new Date(time));
                ((TextView) view).setText(formattedTime);
                return true;
            } else if (view.getId() == R.id.note_category) {
                String category = cursor1.getString(3); // COLUMN_INDEX_CATEGORY
                TextView categoryView = (TextView) view;
                if (category != null && !category.isEmpty()) {
                    categoryView.setText("📁 " + category);
                    categoryView.setVisibility(View.VISIBLE);
                } else {
                    categoryView.setVisibility(View.GONE);
                }
                return true;
            } else if (view.getId() == R.id.note_pinned) {
                int pinned = cursor1.getInt(4); // COLUMN_INDEX_PINNED
                view.setVisibility(pinned == 1 ? View.VISIBLE : View.GONE);
                return true;
            }
            return false;
        });

        mAdapter = adapter;
        setListAdapter(adapter);

        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchResultCount = (TextView) findViewById(R.id.search_result_count);
        
        // Ensure SearchView is expanded and visible
        mSearchView.setIconified(false);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setFocusable(true);
        mSearchView.setFocusableInTouchMode(true);
        
        // Force SearchView to be expanded and show text
        try {
            // Get the search text view inside SearchView
            int searchPlateId = mSearchView.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            if (searchPlateId != 0) {
                EditText searchText = (EditText) mSearchView.findViewById(searchPlateId);
                if (searchText != null) {
                    searchText.setTextColor(getResources().getColor(android.R.color.black));
                    searchText.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }
        } catch (Exception e) {
            // Ignore if we can't access the internal view
            Log.d(TAG, "Could not access SearchView internal EditText", e);
        }
        
        // Setup category filter spinner
        setupCategoryFilter();
        
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Update filter without modifying SearchView text (user just typed it)
                mCurrentFilter = TextUtils.isEmpty(query) ? null : query;
                Cursor newCursor = queryNotes(mCurrentFilter);
                mAdapter.changeCursor(newCursor);
                updateSearchResultCount(newCursor);
                updateFilterContainerVisibility();
                // Keep the search view expanded
                mSearchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update filter without modifying SearchView text (user is typing)
                mCurrentFilter = TextUtils.isEmpty(newText) ? null : newText;
                Cursor newCursor = queryNotes(mCurrentFilter);
                mAdapter.changeCursor(newCursor);
                updateSearchResultCount(newCursor);
                updateFilterContainerVisibility();
                return false; // Return false to allow SearchView to update its text normally
            }
        });

        mSearchView.setOnCloseListener(() -> {
            filterNotes(null);
            mCurrentCategoryFilter = null;
            updateCategoryFilterSpinner();
            // Don't close the search view, keep it expanded
            mSearchView.setIconified(false);
            return true; // Return true to prevent closing
        });
        
        // Add search suggestions button
        mSearchView.setOnSearchClickListener(v -> {
            showSearchOptions();
        });
        
        // Ensure search text is visible by setting query programmatically if needed
        if (mCurrentFilter != null && !mCurrentFilter.isEmpty()) {
            mSearchView.setQuery(mCurrentFilter, false);
        }

        UiPreferences.styleListContainer(
                findViewById(android.R.id.content),
                listView,
                (TextView) findViewById(android.R.id.empty));
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Show/hide clear search button
        MenuItem clearSearchItem = menu.findItem(R.id.menu_clear_search);
        boolean hasFilter = !TextUtils.isEmpty(mCurrentFilter) || 
                           (!TextUtils.isEmpty(mCurrentCategoryFilter) && 
                            !mCurrentCategoryFilter.equals(getString(R.string.all_categories)));
        if (clearSearchItem != null) {
            clearSearchItem.setVisible(hasFilter);
        }

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()).setClassName(/* TODO: provide the application ID. For example: */ getPackageName(), "com.example.android.notepad.NoteEditor"));
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()).setClassName(/* TODO: provide the application ID. For example: */ getPackageName(), "com.example.android.notepad.NoteEditor"));
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_clear_search) {
            // Clear search
            mCurrentFilter = null;
            mCurrentCategoryFilter = null;
            updateCategoryFilterSpinner();
            if (mSearchView != null) {
                mSearchView.setQuery("", false);
                mSearchView.clearFocus();
            }
            filterNotes(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), 
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri).setClassName(/* TODO: provide the application ID. For example: */ getPackageName(), "com.example.android.notepad.NoteEditor"));
            return true;
        } else if (id == R.id.context_copy) { //BEGIN_INCLUDE(copy)
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri));                          // the URI

            // Returns to the caller and skips further processing.
            return true;
            //END_INCLUDE(copy)
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    // passed in.
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        } else if (id == R.id.context_pin) {
            // Toggle pin status
            Cursor noteCursor = getContentResolver().query(noteUri, 
                    new String[]{NotePad.Notes.COLUMN_NAME_PINNED}, null, null, null);
            int currentPinned = 0;
            if (noteCursor != null && noteCursor.moveToFirst()) {
                currentPinned = noteCursor.getInt(0);
                noteCursor.close();
            }
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_PINNED, (currentPinned == 1) ? 0 : 1);
            getContentResolver().update(noteUri, values, null, null);
            return true;
        } else if (id == R.id.context_export) {
            // Export note
            exportNote(noteUri);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void exportNote(Uri noteUri) {
        Cursor cursor = getContentResolver().query(noteUri, 
                new String[]{
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_CATEGORY,
                    NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
                }, null, null, null);
        
        if (cursor == null || !cursor.moveToFirst()) {
            android.widget.Toast.makeText(this, 
                    getString(R.string.export_failed), 
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String title = cursor.getString(0);
        String content = cursor.getString(1);
        String category = cursor.getString(2);
        long createDate = cursor.getLong(3);
        long modifyDate = cursor.getLong(4);
        cursor.close();

        try {
            java.io.File exportDir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOCUMENTS), "NotePad");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", java.util.Locale.getDefault());
            String fileName = "Note_" + sdf.format(new java.util.Date()) + ".txt";
            java.io.File file = new java.io.File(exportDir, fileName);

            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(getString(R.string.export_title) + ": " + title + "\n");
            if (category != null && !category.isEmpty()) {
                writer.write(getString(R.string.menu_category) + ": " + category + "\n");
            }
            sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            writer.write(getString(R.string.export_created) + ": " + 
                    sdf.format(new java.util.Date(createDate)) + "\n");
            writer.write(getString(R.string.export_modified) + ": " + 
                    sdf.format(new java.util.Date(modifyDate)) + "\n");
            writer.write("\n" + getString(R.string.export_content) + ":\n" + content);
            writer.close();

            android.widget.Toast.makeText(this, 
                    getString(R.string.export_success) + ": " + file.getAbsolutePath(), 
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (java.io.IOException e) {
            android.widget.Toast.makeText(this, 
                    getString(R.string.export_failed) + ": " + e.getMessage(), 
                    android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, 
                    getString(R.string.export_failed) + ": " + e.getMessage(), 
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri).setClassName(/* TODO: provide the application ID. For example: */ getPackageName(), "com.example.android.notepad.NoteEditor"));
        }
    }
    private Cursor queryNotes(String filter) {
        String selection = null;
        String[] selectionArgs = null;
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        
        // Text search filter
        if (!TextUtils.isEmpty(filter)) {
            String like = "%" + filter.trim() + "%";
            conditions.add("(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_CATEGORY + " LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        
        // Category filter
        if (!TextUtils.isEmpty(mCurrentCategoryFilter) && 
            !mCurrentCategoryFilter.equals(getString(R.string.all_categories))) {
            conditions.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
            args.add(mCurrentCategoryFilter);
        }
        
        if (!conditions.isEmpty()) {
            selection = TextUtils.join(" AND ", conditions);
            selectionArgs = args.toArray(new String[args.size()]);
        }
        
        // Sort by pinned first, then by modification date
        String sortOrder = NotePad.Notes.COLUMN_NAME_PINNED + " DESC, " + 
                          NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );
        return cursor;
    }

    private void filterNotes(String constraint) {
        mCurrentFilter = TextUtils.isEmpty(constraint) ? null : constraint;
        
        // Update search view to show the current query
        if (mSearchView != null) {
            // Get current query from SearchView to avoid overwriting user input
            String currentQuery = mSearchView.getQuery().toString();
            
            // Only update if the constraint is different from current query
            if (mCurrentFilter != null && !mCurrentFilter.isEmpty()) {
                if (!mCurrentFilter.equals(currentQuery)) {
                    // Set the query text to make it visible
                    mSearchView.setQuery(mCurrentFilter, false);
                }
            } else {
                // Only clear if there's actually text in the search view
                if (!TextUtils.isEmpty(currentQuery)) {
                    mSearchView.setQuery("", false);
                }
            }
            
            // Ensure SearchView stays expanded
            mSearchView.setIconified(false);
            
            // Force the text to be visible by accessing internal EditText
            try {
                int searchPlateId = mSearchView.getContext().getResources()
                        .getIdentifier("android:id/search_src_text", null, null);
                if (searchPlateId != 0) {
                    EditText searchText = (EditText) mSearchView.findViewById(searchPlateId);
                    if (searchText != null) {
                        // Ensure text is visible
                        searchText.setVisibility(View.VISIBLE);
                        if (mCurrentFilter != null && !mCurrentFilter.isEmpty()) {
                            searchText.setText(mCurrentFilter);
                            searchText.setSelection(mCurrentFilter.length());
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore if we can't access the internal view
                Log.d(TAG, "Could not update SearchView internal EditText", e);
            }
        }
        
        Cursor newCursor = queryNotes(mCurrentFilter);
        mAdapter.changeCursor(newCursor);
        
        // Update search result count
        updateSearchResultCount(newCursor);
        
        // Show/hide filter container
        updateFilterContainerVisibility();
    }
    
    private void updateFilterContainerVisibility() {
        View filterContainer = findViewById(R.id.search_filter_container);
        if (filterContainer != null) {
            boolean hasFilter = !TextUtils.isEmpty(mCurrentFilter) || 
                               (!TextUtils.isEmpty(mCurrentCategoryFilter) && 
                                !mCurrentCategoryFilter.equals(getString(R.string.all_categories)));
            filterContainer.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
        }
    }
    
    private void setupCategoryFilter() {
        Spinner categorySpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        if (categorySpinner == null) return;
        
        // Get all categories from database
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                new String[]{NotePad.Notes.COLUMN_NAME_CATEGORY},
                null,
                null,
                null
        );
        
        Set<String> categories = new HashSet<>();
        categories.add(getString(R.string.all_categories));
        
        if (cursor != null) {
            int categoryIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY);
            while (cursor.moveToNext()) {
                String category = cursor.getString(categoryIndex);
                if (category != null && !category.trim().isEmpty()) {
                    categories.add(category);
                }
            }
            cursor.close();
        }
        
        List<String> categoryList = new ArrayList<>(categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                if (selectedCategory.equals(getString(R.string.all_categories))) {
                    mCurrentCategoryFilter = null;
                } else {
                    mCurrentCategoryFilter = selectedCategory;
                }
                // Update filter without modifying SearchView
                Cursor newCursor = queryNotes(mCurrentFilter);
                mAdapter.changeCursor(newCursor);
                updateSearchResultCount(newCursor);
                updateFilterContainerVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentCategoryFilter = null;
            }
        });
    }
    
    private void updateCategoryFilterSpinner() {
        Spinner categorySpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        if (categorySpinner != null && categorySpinner.getAdapter() != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) categorySpinner.getAdapter();
            int position = adapter.getPosition(getString(R.string.all_categories));
            if (position >= 0) {
                categorySpinner.setSelection(position);
            }
        }
    }
    
    private void updateSearchResultCount(Cursor cursor) {
        if (mSearchResultCount == null) return;
        
        boolean hasFilter = !TextUtils.isEmpty(mCurrentFilter) || 
                           (!TextUtils.isEmpty(mCurrentCategoryFilter) && 
                            !mCurrentCategoryFilter.equals(getString(R.string.all_categories)));
        
        if (hasFilter && cursor != null) {
            int count = cursor.getCount();
            String countText = getString(R.string.search_results_count, count);
            mSearchResultCount.setText(countText);
            mSearchResultCount.setVisibility(View.VISIBLE);
        } else {
            mSearchResultCount.setVisibility(View.GONE);
        }
    }
    
    private void showSearchOptions() {
        // This can be expanded to show more search options
        // For now, just ensure the category filter is visible when searching
        View filterContainer = findViewById(R.id.search_filter_container);
        if (filterContainer != null && !TextUtils.isEmpty(mCurrentFilter)) {
            filterContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.styleListContainer(
                findViewById(android.R.id.content),
                getListView(),
                (TextView) findViewById(android.R.id.empty));
        // Refresh category filter list
        setupCategoryFilter();
        // Restore search view state
        if (mSearchView != null && mCurrentFilter != null && !mCurrentFilter.isEmpty()) {
            mSearchView.setQuery(mCurrentFilter, false);
            mSearchView.setIconified(false);
        }
        filterNotes(mCurrentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
        }
    }
}
