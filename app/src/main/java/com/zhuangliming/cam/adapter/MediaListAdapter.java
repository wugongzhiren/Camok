package com.zhuangliming.cam.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zhuangliming.cam.R;
import com.zhuangliming.cam.model.MediaItem;

import java.util.List;

public class MediaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    private List<MediaItem> datas;

    //对外设置item点击的接口
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickLitener(OnItemClickListener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    private OnItemClickListener mOnItemClickLitener;

    public MediaListAdapter(Context context, List<MediaItem> photos) {
        this.mContext=context;
        this.datas=photos;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        if(i==0){
             view = View.inflate(mContext, R.layout.photo_item, null);
             return new MyPhotoViewHolder(view);
        }
        else{
            view = View.inflate(mContext, R.layout.video_item, null);
            return new MyVideoViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder myViewHolder, final int i) {
        if(myViewHolder instanceof MyPhotoViewHolder){
            Glide.with(mContext)
                    .load(datas.get(i).url)
                    .thumbnail(/*sizeMultiplier=*/ 0.25f)
                    .into(((MyPhotoViewHolder)myViewHolder).imageView);
            ((MyPhotoViewHolder)myViewHolder).textView.setText(datas.get(i).name);
        }
        if(myViewHolder instanceof MyVideoViewHolder){
            Glide.with(mContext)
                    .load(datas.get(i).url)
                    .thumbnail(/*sizeMultiplier=*/ 0.25f)
                    .into(((MyVideoViewHolder)myViewHolder).imageView);
      /*  Glide.with(mContext)
                .load(datas.get(i).url)
                .into(myViewHolder.imageView);*/
            ((MyVideoViewHolder)myViewHolder).textView.setText(datas.get(i).name);
        }
        myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickLitener.onItemClick( myViewHolder.itemView,i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    @Override
    public int getItemViewType(int position) {

        return datas.get(position).type;


    }
    public void setDatas(List<MediaItem> mediaItems){
        this.datas.clear();
        this.datas=mediaItems;
        notifyDataSetChanged();
    }
     class MyPhotoViewHolder extends RecyclerView.ViewHolder {
       public ImageView imageView;
       public TextView textView;

        public MyPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView=itemView.findViewById(R.id.thumbnail);
            textView=itemView.findViewById(R.id.photoName);
        }
    }
    class MyVideoViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;

        public MyVideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView=itemView.findViewById(R.id.thumbnail);
            textView=itemView.findViewById(R.id.videoName);
        }
    }
}
