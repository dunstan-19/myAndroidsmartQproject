package com.example.hello;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {
    private List<String> departmentList;
    private Context context;

    public DepartmentAdapter(List<String> departmentList, Context context) {
        this.departmentList = departmentList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String department = departmentList.get(position);
        holder.departmentName.setText(department);

        holder.departmentCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, QueueManagementActivity.class);
            intent.putExtra("department", department); // Pass the department name
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return departmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView departmentName;
        CardView departmentCard;

        public ViewHolder(View itemView) {
            super(itemView);
            departmentName = itemView.findViewById(R.id.departmentName);
            departmentCard = itemView.findViewById(R.id.departmentCard);
        }
    }
}