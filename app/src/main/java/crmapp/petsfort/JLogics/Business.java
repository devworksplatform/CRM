package crmapp.petsfort.JLogics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import crmapp.petsfort.JLogics.Models.Category;
import crmapp.petsfort.JLogics.Models.User;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import crmapp.petsfort.JLogics.Models.Product;
import crmapp.petsfort.R;

public class Business {
//    private static final String ServerURL = "http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000";
    private static final String ServerURL = "https://server.petsfort.in";
    Context context;
    public Business(Context context) {
        this.context = context;
    }

    public enum JOrderStatus {
        ORDER_PENDING("ORDER_PENDING", "#FFFF00", "Pending", R.drawable.shape_status_background_pending),
        ORDER_IN_PROGRESS("ORDER_IN_PROGRESS", "#0000FF", "In Progress", R.drawable.shape_status_background_progress),
        ORDER_DELIVERED("ORDER_DELIVERED", "#008000", "Delivered", R.drawable.shape_status_background_delivered),
        ORDER_CANCELLED("ORDER_CANCELLED", "#FF0000", "Cancelled", R.drawable.shape_status_background_canceled);

        private final String status;
        private final String colorHex;
        private final String visibleText;
        private final int drawableRes;

        JOrderStatus(String status, String colorHex, String visibleText, int drawableRes) {
            this.status = status;
            this.colorHex = colorHex;
            this.visibleText = visibleText;
            this.drawableRes = drawableRes;
        }

        public String getStatus() {
            return status;
        }

        public String getColor() {
            return colorHex;
        }

        public String getVisibleText() {
            return visibleText;
        }

        public int getDrawableRes() {
            return drawableRes;
        }

        @Override
        public String toString() {
            return status;
        }
    }



    public static class JFCM {
        private static HashMap<String,String> fcmTopics;
        static {
            fcmTopics = new HashMap<>();
        }

        public static void subscribeToTopic(@NonNull final String topic, final OnCompleteListener<Void> listener) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                fcmTopics.put(topic, topic);
                            }
                            listener.onComplete(task);
                        }
                    });
        }

        public static void unSubscribeToTopic(@NonNull final String topic, final OnCompleteListener<Void> listener) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                fcmTopics.remove(topic);
                            }
                            listener.onComplete(task);
                        }
                    });
        }

        public static void unSubscribeAll() {
            for(String topic : fcmTopics.keySet()) {
                unSubscribeToTopic(topic, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
            }
            fcmTopics.clear();
        }


        public static void subscribeBasicTopics(@NonNull String userId, @NonNull String role, final OnCompleteListener<Void> listener) {
            // Define basic topics based on user details. Customize topic names as needed.
            final String userTopic = "user_" + userId;      // e.g., user_12345
            final String roleTopic = "role_" + role.toLowerCase(); // e.g., role_admin, role_user
            final String allUsersTopic = "all_users";       // General topic for all users

            // Subscribe to each basic topic.
            // We pass null for the listener here, meaning the caller of subscribeBasicTopics
            // won't be directly notified of individual subscription successes/failures.
            // The fcmTopics map will be updated asynchronously in the subscribeToTopic callbacks.
            subscribeToTopic(allUsersTopic, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
            subscribeToTopic(roleTopic, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
            subscribeToTopic(userTopic, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    listener.onComplete(task);
                }
            });
        }




    }



    public static class localDB_SharedPref {

        public static String PROXY_KEY = "Proxy";
        public static String PREF_KEY = "localDB";

        public static void setProxyUID(SharedPreferences localDB, String userId) {
            SharedPreferences.Editor editor = localDB.edit();
            editor.putString(PROXY_KEY, userId);
            editor.apply();
        }

        public static String getProxyUID(SharedPreferences localDB, String userId) {
            String proxy_uid = localDB.getString(PROXY_KEY, null);
            if(proxy_uid == null) {
                return userId;
            } else {
                return proxy_uid;
            }
        }



        // Method to save the HashMap
        public static void saveHashMap(SharedPreferences localDB, HashMap<String, Object> map) {
            SharedPreferences.Editor editor = localDB.edit();
            Gson gson = new Gson();
            String json = gson.toJson(map); // Serialize HashMap to JSON String
            editor.putString(PREF_KEY, json);
            editor.apply(); // Use apply() for asynchronous saving
        }

        // Method to retrieve the HashMap
        public static HashMap<String, Object> getHashMap(SharedPreferences localDB) {
            Gson gson = new Gson();
            String json = localDB.getString(PREF_KEY, null); // Get the JSON string

            if (json == null) {
                HashMap<String,Object> temp = new HashMap<>();
                saveHashMap(localDB, temp); // Save an empty HashMap if no data was found);
                return temp; // Return an empty map if no data was saved
            }

            try {
                return gson.fromJson(json, new TypeToken<HashMap<String, Object>>() {}.getType()); // Deserialize JSON String to HashMap
            } catch (Exception e) {
                HashMap<String,Object> temp = new HashMap<>();
                saveHashMap(localDB, temp); // Save an empty HashMap if no data was found);
                return temp; // Return an empty map if no data was saved
            }
        }

        public static void updateCartProduct(SharedPreferences localDB, String productID, HashMap<String,Object> data) {
            HashMap<String,Object> details = getHashMap(localDB);
            HashMap<String,Object> carts = null;
            try {
                LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get("carts");
                if(temp == null) {
                    temp = new LinkedTreeMap<>();
                }
                carts = new HashMap<String,Object>(temp);
            } catch (Exception e) {
                carts = new HashMap<>();
            }

            carts.put(productID, data);
            details.put("carts",carts);
            saveHashMap(localDB, details);
        }

        public static void  deleteCartProduct(SharedPreferences localDB, String productID) {
            HashMap<String,Object> details = getHashMap(localDB);

            if(details.containsKey("carts")) {
                HashMap<String,Object> carts = null;
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get("carts");
                    if(temp == null) {
                        temp = new LinkedTreeMap<>();
                    }
                    carts = new HashMap<String,Object>(temp);
                } catch (Exception e) {
                    carts = new HashMap<>();
                }

                if(carts.containsKey(productID)) {
                    carts.remove(productID);
                    details.put("carts",carts);
                    saveHashMap(localDB, details);
                }
            }
        }

        public static HashMap<String,Object> getCartProduct(SharedPreferences localDB, String productID) {
            HashMap<String,Object> details = getHashMap(localDB);

            if(details.containsKey("carts")) {
                HashMap<String,Object> carts = null;
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get("carts");
                    if(temp == null) {
                        temp = new LinkedTreeMap<>();
                    }
                    carts = new HashMap<String,Object>(temp);
                } catch (Exception e) {
                    carts = new HashMap<>();
                }

                if(carts.containsKey(productID)) {
                    try{
                        LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) carts.get(productID);
                        return new HashMap<String,Object>(temp);
                    } catch (Exception e) {
                        return new HashMap<>();
                    }
                } else {
                    return new HashMap<>();
                }
            }

            return new HashMap<>();
        }

        public static HashMap<String,Object> getCart(SharedPreferences localDB) {
            HashMap<String,Object> details = getHashMap(localDB);

            HashMap<String,Object> carts = new HashMap<>();
            if(details.containsKey("carts")) {
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get("carts");
                    if(temp == null) {
                        temp = new LinkedTreeMap<>();
                    }
                    carts = new HashMap<String,Object>(temp);
                } catch (Exception e) {
                    carts = new HashMap<>();
                }
            }

            return carts;
        }

        public static void clearCart(SharedPreferences localDB) {
            HashMap<String,Object> details = getHashMap(localDB);
            if(details.containsKey("carts")) {
                details.remove("carts");
                saveHashMap(localDB, details);
            }
        }
    }


    public static class UserDataApiClient {
        private static final OkHttpClient client = new OkHttpClient();
        private static final String getURL = ServerURL + "/user/";
        private static final String URL = ServerURL + "/userdata/";

        public static void putUserDataCallApi(String userId, User user, Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    HashMap<String,Object> dataMap = new HashMap<>();
                    dataMap.put("name",user.name);
                    dataMap.put("email",user.email);
                    dataMap.put("role",user.role);
                    dataMap.put("address",user.address);
                    dataMap.put("credits",user.credits);
                    dataMap.put("isblocked",user.isBlocked);
                    dataMap.put("pwd","");

                    JSONObject jsonBody = new JSONObject(dataMap);
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

                    Request request = new Request.Builder()
                            .url(URL + userId)
                            .addHeader("Content-Type", "application/json")
                            .put(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (responseBody.contains("successfully")) {
                            callback.onReceived(new UserDataApiResponse(response.code(), user));
                        } else {
                            callback.onReceived(new UserDataApiResponse(500, (User) null));
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new UserDataApiResponse(500, (User) null));
                    });
                }
            });
        }
        public static void getUserDataCallApi(String userId, Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(getURL + userId)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    User user = parseUser(responseBody);

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new UserDataApiResponse(response.code(), user));
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new UserDataApiResponse(500, (User) null));
                    });
                }
            });
        }

        public static void getAllUsersCallApi(Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(URL) // already set as "/userdata/"
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    List<User> users = parseUsers(responseBody);

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new UserDataApiResponse(response.code(), users));
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new UserDataApiResponse(500, (List<User>) null));
                    });
                }
            });
        }

        public static List<User> parseUsers(String responseBody) {
            List<User> userList = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(responseBody);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject userObject = jsonArray.getJSONObject(i);
                    User user = parseUser(userObject.toString());
                    if (user != null) {
                        userList.add(user);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return userList;
        }



        public static User parseUser(String responseBody) {
            try {
                JSONObject obj = new JSONObject(responseBody);
                String uid = obj.getString("uid");
                String id = obj.getString("id");
                String name = obj.getString("name");
                String email = obj.getString("email");
                String role = obj.getString("role");
                String address = obj.getString("address");
                double credits = obj.getDouble("credits");
                int isBlocked = obj.getInt("isblocked");

                return new User(uid, id, name, email, role, address, credits, isBlocked);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static class UserDataApiResponse {
            private final int statusCode;
            private final User user;
            private final List<User> users;

            public UserDataApiResponse(int statusCode, User user) {
                this.statusCode = statusCode;
                this.user = user;
                this.users = null;
            }

            public UserDataApiResponse(int statusCode, List<User> users) {
                this.statusCode = statusCode;
                this.user = null;
                this.users = users;
            }

            public int getStatusCode() { return statusCode; }
            public User getUser() { return user; }
            public List<User> getUsers() { return users; }
        }
    }

    public static class CategoriesApiClient {
        private static final String URL = ServerURL + "/categories";
        private static final OkHttpClient client = new OkHttpClient();

        public static void getCategoriesCallApi(Callbacker.ApiResponseWaiters.CategoriesApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(URL)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    ArrayList<Category> categoryList = parseCategories(responseBody);
                    // Switch back to the main thread to update the UI
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new CategoriesApiClient.CategoriesApiResponse(response.code(), categoryList));
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    // Switch back to the main thread to update the UI
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new CategoriesApiClient.CategoriesApiResponse(500,new ArrayList<>()));
                    });
                }
            });
        }

        public static ArrayList<Category> parseCategories(String responseBody) {
            ArrayList<Category> categories = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(responseBody);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String image = obj.getString("image");

                    Category category = new Category(id,name,image);
                    categories.add(category);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return categories;
        }

        public static class CategoriesApiResponse {
            private final int statusCode;
            private final ArrayList<Category> categories;

            public CategoriesApiResponse(int statusCode, ArrayList<Category> categories) {
                this.statusCode = statusCode;
                this.categories = categories;
            }

            public ArrayList<Category> getCategories() {return categories;}
            public int getStatusCode() {
                return statusCode;
            }
        }

    }

    public static class QueryApiClient {
        private static final String URL = ServerURL + "/products/query";
        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, Callbacker.ApiResponseWaiters.QueryApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    JSONObject jsonBody = new JSONObject(data);
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(URL)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    ArrayList<Product> productList = parseProducts(responseBody);
                    // Switch back to the main thread to update the UI
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new QueryApiResponse(response.code(), productList));
                    });

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // Switch back to the main thread to update the UI
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new QueryApiResponse(500, new ArrayList<>()));
                    });
                }
            });
        }
        private ArrayList<Product> parseProducts(String responseBody) throws JSONException {
            ArrayList<Product> products = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(responseBody);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Product product = new Product();
                product.setProductId(jsonObject.optString("product_id"));
                product.setProductName(jsonObject.optString("product_name"));
                product.setProductDesc(jsonObject.optString("product_desc"));
                product.setProductImg(parseJsonArrayToList(jsonObject.optJSONArray("product_img")));
                product.setCatId(jsonObject.optString("cat_id"));
                product.setCatSub(jsonObject.optString("cat_sub"));
                product.setCostRate(jsonObject.optDouble("cost_rate"));
                product.setCostMrp(jsonObject.optDouble("cost_mrp"));
                product.setCostGst(jsonObject.optDouble("cost_gst"));
                product.setCostDis(jsonObject.optDouble("cost_dis"));
                product.setStock(jsonObject.optInt("stock"));
                product.setId(jsonObject.optString("id"));

                products.add(product);
            }
            return products;
        }

        public class QueryApiResponse {
            private int statusCode;
            private ArrayList<Product> products;

            public QueryApiResponse(int statusCode, ArrayList<Product> products) {
                this.statusCode = statusCode;
                this.products = products;
            }

            public int getStatusCode() { return statusCode; }
            public ArrayList<Product> getProducts() { return products; }
        }



        private List<String> parseJsonArrayToList(JSONArray jsonArray) {
            List<String> list = new ArrayList<>();
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.optString(i));
                }
            }
            return list;
        }

    }
    public static class BulkDetailsApiClient {
        private static final String URL = ServerURL + "/products/bulk-details";
        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, Callbacker.ApiResponseWaiters.BulkDetailsApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    JSONObject jsonBody = new JSONObject(data);
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(URL)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    BulkDetailsApiResponse apiResponse = parseResponse(responseBody);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new BulkDetailsApiResponse(response.code(), apiResponse));
                    });

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new BulkDetailsApiResponse(500, new BulkDetailsApiResponse()));
                    });
                }
            });
        }

        private BulkDetailsApiResponse parseResponse(String responseBody) throws JSONException {
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray productArray = jsonObject.optJSONArray("product_details");
            JSONObject costObject = jsonObject.optJSONObject("cost");

            ArrayList<Product> products = new ArrayList<>();
            if (productArray != null) {
                for (int i = 0; i < productArray.length(); i++) {
                    JSONObject productJson = productArray.getJSONObject(i);
                    Product product = new Product();
                    product.setProductId(productJson.optString("product_id"));
                    product.setProductName(productJson.optString("product_name"));
                    product.setProductDesc(productJson.optString("product_desc"));
                    product.setProductImg(parseJsonArrayToList(productJson.optJSONArray("product_img")));
                    product.setCatId(productJson.optString("cat_id"));
                    product.setCatSub(productJson.optString("cat_sub"));
                    product.setCostRate(productJson.optDouble("cost_rate"));
                    product.setCostMrp(productJson.optDouble("cost_mrp"));
                    product.setCostGst(productJson.optDouble("cost_gst"));
                    product.setCostDis(productJson.optDouble("cost_dis"));
                    product.setStock(productJson.optInt("stock"));
                    product.setId(productJson.optString("id"));
//                    product.setCreatedAt(productJson.optString("created_at"));
//                    product.setUpdatedAt(productJson.optString("updated_at"));
                    products.add(product);
                }
            }

            CostDetails costDetails = new CostDetails(
                    costObject != null ? costObject.optDouble("total_rate", 0.0) : 0.0,
                    costObject != null ? costObject.optDouble("total_gst", 0.0) : 0.0,
                    costObject != null ? costObject.optDouble("total", 0.0) : 0.0,
                    costObject != null ? costObject.optDouble("total_discount", 0.0) : 0.0
            );

            return new BulkDetailsApiResponse(products, costDetails);
        }

        public class BulkDetailsApiResponse {
            private ArrayList<Product> products;
            private CostDetails costDetails;
            private int statusCode;

            public BulkDetailsApiResponse(ArrayList<Product> products, CostDetails costDetails) {
                this.products = products;
                this.costDetails = costDetails;
            }

            public BulkDetailsApiResponse(int statusCode, BulkDetailsApiResponse response) {
                this.statusCode = statusCode;
                this.products = response.getProducts();
                this.costDetails = response.getCostDetails();
            }

            public BulkDetailsApiResponse() {
                this.products = new ArrayList<>();
                this.costDetails = new CostDetails(0.0, 0.0, 0.0, 0.0);
            }

            public ArrayList<Product> getProducts() { return products; }
            public CostDetails getCostDetails() { return costDetails; }
            public int getStatusCode() { return statusCode; }
        }

        public static class CostDetails {
            private final double totalRate;
            private final double totalGst;
            private final double total;
            private final double totalDiscount;

            public CostDetails(double totalRate, double totalGst, double total, double totalDiscount) {
                this.totalRate = totalRate;
                this.totalGst = totalGst;
                this.total = total;
                this.totalDiscount = totalDiscount;
            }

            public double getTotalRate() { return totalRate; }
            public double getTotalGst() { return totalGst; }
            public double getTotalDiscount() { return totalDiscount; }
            public double getTotal() { return total; }
        }

        private List<String> parseJsonArrayToList(JSONArray jsonArray) {
            List<String> list = new ArrayList<>();
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.optString(i));
                }
            }
            return list;
        }
    }
    public static class OrderCheckoutApiClient {

        // Define ServerURL here or pass it in constructor/method if needed
        private static final String BASE_URL = ServerURL + "/orders/checkout/"; // Note the trailing slash
        private final OkHttpClient client = new OkHttpClient();

        /**
         * Calls the order checkout API.
         * The API now returns a simple status ("stored" or "failed").
         *
         * @param userId   The ID of the user placing the order.
         * @param data     A HashMap representing the order payload, e.g., {"product_id1": {"count": 2}}
         * @param callback The callback to handle the API response.
         */
        public void callApi(String userId, HashMap<String, Object> data, Callbacker.ApiResponseWaiters.OrderCheckoutApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                String url = BASE_URL + userId; // Append user ID to the base URL
                try {
                    JSONObject jsonBody = new JSONObject(data);
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(url) // Use the constructed URL with user ID
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    int statusCode = response.code();

                    OrderCheckoutApiResponse apiResponse = null;

                    // According to the *new* API spec, even errors might return HTTP 200.
                    // We need to parse the body regardless of statusCode to get the "status" field.
                    String parsedStatus = null;
                    String errorMessage = null; // Store potential error message

                    try {
                        JSONObject responseObject = new JSONObject(responseBody);
                        parsedStatus = responseObject.optString("order_status", null); // Get the status field
                        if (parsedStatus == null) {
                            // If status field is missing, treat it as an unexpected response format
                            errorMessage = "API response missing 'status' field.";
                            if (statusCode >= 200 && statusCode < 300) statusCode = 500; // Treat as server error if status was 2xx but format wrong
                        }
                    } catch (JSONException e) {
                        // Failed to parse JSON - means unexpected response format
                        System.err.println("Order Checkout API - JSON Parsing Error: " + e.getMessage());
                        errorMessage = "Invalid JSON response from server.";
                        if (statusCode >= 200 && statusCode < 300) statusCode = 500; // Treat as server error if status was 2xx but format wrong
                        parsedStatus = "failed"; // Default to failed if parsing fails
                    }

                    // Create the response object using the parsed status and original HTTP code
                    apiResponse = new OrderCheckoutApiResponse(statusCode, parsedStatus, errorMessage);


                    // Post result back to the main thread
                    OrderCheckoutApiResponse finalApiResponse = apiResponse; // Need final variable for lambda
//                    callback.onReceived(finalApiResponse);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onReceived(finalApiResponse);
                    });

                } catch (IOException e) {
                    // Handle exceptions during request execution (Network error)
                    System.err.println("Order Checkout API - Network Error: " + e.getMessage());
                    e.printStackTrace();
                    // Create error response for client-side network issue
                    OrderCheckoutApiResponse errorResponse = new OrderCheckoutApiResponse(503, "failed", "Network Error: " + e.getMessage()); // 503 Service Unavailable might fit
//                    callback.onReceived(errorResponse);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onReceived(errorResponse);
                    });

                } catch (Exception e) {
                    // Handle exceptions during JSON conversion of the *request* body
                    System.err.println("Order Checkout API - Request JSON Error: " + e.getMessage());
                    e.printStackTrace();
                    OrderCheckoutApiResponse errorResponse = new OrderCheckoutApiResponse(400, "failed", "Invalid request data format."); // 400 Bad Request
//                    callback.onReceived(errorResponse);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onReceived(errorResponse);
                    });

                }
            });
        }

        /**
         * Data class to hold the simplified response from the Order Checkout API.
         */
        public class OrderCheckoutApiResponse {
            private final int statusCode;
            private final String status; // "stored" or "failed" (or null if parsing fails)
            private final String errorMessage; // For client-side or parsing errors

            /**
             * Constructor for the checkout API response.
             *
             * @param statusCode   The HTTP status code received.
             * @param status       The value of the "status" field from the response JSON ("stored", "failed", or null).
             * @param errorMessage An optional error message for client-side/parsing issues.
             */
            public OrderCheckoutApiResponse(int statusCode, String status, String errorMessage) {
                this.statusCode = statusCode;
                this.status = status;
                this.errorMessage = errorMessage;
            }

            /**
             * Gets the raw HTTP status code.
             */
            public int getStatusCode() {
                return statusCode;
            }

            /**
             * Gets the status message from the API response body ("stored" or "failed").
             * Can be null if the response format was unexpected.
             */
            public String getStatus() {
                return status;
            }

            /**
             * Gets any error message generated during client-side processing or parsing.
             * Null if no such error occurred.
             */
            public String getErrorMessage() {
                return errorMessage;
            }

            /**
             * Checks if the operation was successful according to the API logic.
             * Requires both a 2xx HTTP status code AND the status field to be "stored".
             *
             * @return true if the order was successfully stored, false otherwise.
             */
            public boolean isSuccessful() {
                // Check HTTP status AND the API's own status field
                return statusCode >= 200 && statusCode < 300;
            }

            /**
             * Checks specifically if the API reported a "failed" status in its response body.
             * This is independent of the HTTP status code, as the API might return HTTP 200 OK
             * even when reporting {"status": "failed"}.
             * @return true if the API response body contained {"status": "failed"}
             */
            public boolean isApiReportedFailure() {
                return "failed".equalsIgnoreCase(status);
            }
        }


    }
    public static class OrderQueryApiClient {
        private static final String URL = ServerURL + "/orders/query";
        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, OrderApiCallback callback) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    JSONObject jsonBody = new JSONObject(data);
                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(URL)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    ArrayList<Order> orderList = parseOrders(responseBody);
                    // Switch back to the main thread to update the UI
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new OrderQueryApiResponse(response.code(), orderList));
                    });

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // Switch back to the main thread to update the UI
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onReceived(new OrderQueryApiResponse(500, new ArrayList<>()));
                    });
                }
            });
        }

        private ArrayList<Order> parseOrders(String responseBody) throws JSONException {
            ArrayList<Order> orders = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(responseBody);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Order order = new Order();
                order.setOrderId(jsonObject.optString("order_id"));
                order.setUserId(jsonObject.optString("user_id"));

                JSONObject itemsJson = jsonObject.optJSONObject("items");
                if (itemsJson != null) {
                    Map<String, Map<String, Object>> itemsMap = new HashMap<>();
                    java.util.Iterator<String> keys = itemsJson.keys();
                    while (keys.hasNext()) {
                        String productId = keys.next();
                        JSONObject countJson = itemsJson.optJSONObject(productId);
                        if (countJson != null) {
                            Map<String, Object> countMap = new HashMap<>();
                            countMap.put("count", countJson.optInt("count"));
                            itemsMap.put(productId, countMap);
                        }
                    }
                    order.setItems(itemsMap);
                }

                JSONArray itemsDetailJsonArray = jsonObject.optJSONArray("items_detail");
                if (itemsDetailJsonArray != null) {
                    ArrayList<Product> itemsDetailList = new ArrayList<>();
                    for (int j = 0; j < itemsDetailJsonArray.length(); j++) {
                        JSONObject itemDetailJson = itemsDetailJsonArray.getJSONObject(j);
                        Product product = new Product();
                        product.setId(itemDetailJson.optString("id"));
                        product.setProductId(itemDetailJson.optString("product_id"));
                        product.setProductName(itemDetailJson.optString("product_name"));
                        product.setProductDesc(itemDetailJson.optString("product_desc"));

                        String productImgString = itemDetailJson.optString("product_img");
                        if (productImgString != null && !productImgString.isEmpty()) {
                            try{
                                JSONArray productImgArray = new JSONArray(productImgString);
                                product.setProductImg(parseJsonArrayToList(productImgArray));
                            } catch (Exception e) {
                                product.setProductImg(null);
                            }
                        }

                        product.setCatId(itemDetailJson.optString("cat_id"));
                        product.setCatSub(itemDetailJson.optString("cat_sub"));
                        product.setCostRate(itemDetailJson.optDouble("cost_rate"));
                        product.setCostMrp(itemDetailJson.optDouble("cost_mrp"));
                        product.setCostGst(itemDetailJson.optDouble("cost_gst"));
                        product.setCostDis(itemDetailJson.optDouble("cost_dis"));
                        product.setStock(itemDetailJson.optInt("stock"));
                        itemsDetailList.add(product);
                    }
                    order.setItemsDetail(itemsDetailList);
                }

                order.setOrderStatus(jsonObject.optString("order_status"));
                order.setTotalRate(jsonObject.optDouble("total_rate"));
                order.setTotalGst(jsonObject.optDouble("total_gst"));
                order.setTotalDiscount(jsonObject.optDouble("total_discount"));
                order.setTotal(jsonObject.optDouble("total"));
                order.setCreatedAt(jsonObject.optString("created_at"));

                orders.add(order);
            }
            return orders;
        }

        public static class OrderQueryApiResponse {
            private int statusCode;
            private ArrayList<Order> orders;

            public OrderQueryApiResponse(int statusCode, ArrayList<Order> orders) {
                this.statusCode = statusCode;
                this.orders = orders;
            }

            public int getStatusCode() {
                return statusCode;
            }

            public ArrayList<Order> getOrders() {
                return orders;
            }
        }

        public interface OrderApiCallback {
            void onReceived(OrderQueryApiResponse response);
        }

        public static class Order implements Serializable {
            private String orderId;
            private String userId;
            private Map<String, Map<String, Object>> items;
            private List<Product> itemsDetail; // Changed to List<Product>
            private String orderStatus;
            private double totalRate;
            private double totalGst;
            private double totalDiscount;
            private double total;
            private String createdAt;

            public String getOrderId() { return orderId; }
            public void setOrderId(String orderId) { this.orderId = orderId; }
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
            public Map<String, Map<String, Object>> getItems() { return items; }
            public void setItems(Map<String, Map<String, Object>> items) { this.items = items; }
            public List<Product> getItemsDetail() { return itemsDetail; }
            public void setItemsDetail(List<Product> itemsDetail) { this.itemsDetail = itemsDetail; }
            public String getOrderStatus() { return orderStatus; }
            public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
            public double getTotalRate() { return totalRate; }
            public void setTotalRate(double totalRate) { this.totalRate = totalRate; }
            public double getTotalGst() { return totalGst; }
            public void setTotalGst(double totalGst) { this.totalGst = totalGst; }
            public double getTotalDiscount() { return totalDiscount; }
            public void setTotalDiscount(double totalDiscount) { this.totalDiscount = totalDiscount; }
            public double getTotal() { return total; }
            public void setTotal(double total) { this.total = total; }
            public String getCreatedAt() {
                return JHelpers.convertUtcToIstAndFormat(createdAt);
            }
            public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

            public BulkDetailsApiClient.CostDetails getCostDetails() {
                return new BulkDetailsApiClient.CostDetails(this.totalRate, this.totalGst, this.total, this.totalDiscount);
            }
        }

        private List<String> parseJsonArrayToList(JSONArray jsonArray) {
            List<String> list = new ArrayList<>();
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.optString(i));
                }
            }
            return list;
        }
    }
}
