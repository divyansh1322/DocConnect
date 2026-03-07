package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * ARTICLE ADAPTER
 * Responsible for inflating article cards and handling the logic to redirect
 * users to external health resources via the device's web browser.
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private List<ArticleModel> articleList;
    private Context context;

    public ArticleAdapter(List<ArticleModel> articleList, Context context) {
        this.articleList = articleList;
        this.context = context;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the custom card layout (item_article) for each row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        ArticleModel article = articleList.get(position);

        // --- Data Binding ---
        holder.tvTitle.setText(article.getTitle());
        holder.tvCategory.setText(article.getCategory());
        holder.tvDate.setText(article.getDate());

        // Loads image from local drawable resources
        holder.imgArticle.setImageResource(article.getImageResId());

        // --- IMPLICIT INTENT LOGIC ---
        // A2Z COMMENT: Instead of a new activity, we trigger an ACTION_VIEW.
        // This tells Android to find any app (like Chrome or Safari) that can handle URLs.
        holder.itemView.setOnClickListener(v -> {
            String url = article.getArticleUrl();
            if (url != null && !url.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                /*
                 * SAFETY TIP: We use v.getContext() to ensure the Intent is
                 * attached to a valid UI context during the transition.
                 */
                v.getContext().startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articleList != null ? articleList.size() : 0;
    }

    /**
     * ViewHolder acts as a cache for the view references.
     * This prevents unnecessary calls to findViewById(), significantly improving scroll performance.
     */
    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvDate;
        ImageView imgArticle;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            imgArticle = itemView.findViewById(R.id.imgArticle);
        }
    }
}