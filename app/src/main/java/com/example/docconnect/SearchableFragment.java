package com.example.docconnect;

/**
 * ARCHITECTURAL CONTRACT:
 * This interface allows the Activity (which has the SearchBar)
 * to talk to the Fragment (which has the List) safely.
 */
public interface SearchableFragment {

    /**
     * The implementation inside the Fragment will handle
     * the actual filtering of the RecyclerView adapter.
     * @param query The text typed into the search bar.
     */
    void filterList(String query);
}