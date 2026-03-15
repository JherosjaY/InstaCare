package com.example.instacare;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private AdminUsersAdapter adapter;
    private AppDatabase db;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvUsers = view.findViewById(R.id.rvUsers);
        emptyState = view.findViewById(R.id.emptyStateUsers);
        TextInputEditText etSearch = view.findViewById(R.id.etSearchUsers);

        adapter = new AdminUsersAdapter(new ArrayList<>(), new AdminUsersAdapter.Listener() {
            @Override
            public void onSuspend(User user) {
                handleSuspend(user);
            }
            @Override
            public void onDelete(User user) {
                confirmDelete(user);
            }
            @Override
            public void onUserClick(User user) {
                showUserDetails(user);
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> users = db.userDao().getAllUsers();
            final List<User> result = users != null ? users : new ArrayList<>();
            requireActivity().runOnUiThread(() -> updateList(result));
        }).start();
    }

    private void searchUsers(String query) {
        new Thread(() -> {
            List<User> users;
            if (query.isEmpty()) {
                users = db.userDao().getAllUsers();
            } else {
                users = db.userDao().searchUsers(query);
            }
            final List<User> result = users != null ? users : new ArrayList<>();
            requireActivity().runOnUiThread(() -> updateList(result));
        }).start();
    }

    private void updateList(List<User> users) {
        adapter.updateList(users);
        if (users.isEmpty()) {
            rvUsers.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvUsers.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleSuspend(User user) {
        new Thread(() -> {
            if (user.isSuspended) {
                db.userDao().unsuspendUser(user.email);
            } else {
                db.userDao().suspendUser(user.email);
            }
            requireActivity().runOnUiThread(() -> {
                String msg = user.isSuspended ? "Account unsuspended" : "Account suspended";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                loadUsers();
            });
        }).start();
    }

    private void confirmDelete(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Permanently delete " + user.username + "? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    new Thread(() -> {
                        db.userDao().deleteUserByEmail(user.email);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "User deleted", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUserDetails(User user) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_details, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvDetailsTitle);
        TextView tvEmail = view.findViewById(R.id.tvDetailsUserEmail);
        TextView tvFullName = view.findViewById(R.id.tvDetailsFullName);
        TextView tvPhone = view.findViewById(R.id.tvDetailsPhone);
        View btnClose = view.findViewById(R.id.btnCloseDetails);

        tvTitle.setText(user.username != null ? user.username + " Details" : "User Details");
        tvEmail.setText(user.email);
        tvFullName.setText(user.fullName != null && !user.fullName.isEmpty() ? user.fullName : "Not provided");
        tvPhone.setText(user.phone != null && !user.phone.isEmpty() ? user.phone : "Not provided");

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
