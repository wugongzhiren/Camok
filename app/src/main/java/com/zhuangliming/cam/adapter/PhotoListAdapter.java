package com.zhuangliming.cam.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zhuangliming.cam.R;
import com.zhuangliming.cam.model.MediaItem;

import java.util.List;

public class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.MyViewHolder> {
    private Context mContext;
    private List<MediaItem> datas;
    public PhotoListAdapter(Context context, List<MediaItem> photos) {
        this.mContext=context;
        this.datas=photos;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = View.inflate(mContext, R.layout.photp_item, null);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {
        Glide.with(mContext)
                .load(datas.get(i).url)
                .into(myViewHolder.imageView);

    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public void setDatas(List<MediaItem> mediaItems){
        this.datas.clear();
        this.datas=mediaItems;
        notifyDataSetChanged();
    }
     class MyViewHolder extends RecyclerView.ViewHolder {
       public ImageView imageView;
       public TextView textView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView=itemView.findViewById(R.id.thumbnail);
            textView=itemView.findViewById(R.id.photoName);
        }


    }

}
