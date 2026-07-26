package crmapp.petsfort.JLogics;

import android.content.Context;
import android.content.SharedPreferences;

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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crmapp.petsfort.JLogics.Models.Category;
import crmapp.petsfort.JLogics.Models.SubCategory;
import crmapp.petsfort.JLogics.Models.User;
import crmapp.petsfort.JLogics.Models.Product;
import crmapp.petsfort.R;

public class Business {
    private static volatile Context applicationContext;
    Context context;
    public Business(Context context) {
        this.context = context;
        initializeServerUrl(context);
    }

    public static void initializeServerUrl(Context context) {
        if (context == null) {
            return;
        }

        applicationContext = context.getApplicationContext();
    }

    private static PetsFortJrpcClient rpcClient() {
        if (applicationContext == null) {
            throw new IllegalStateException("Business has not been initialized.");
        }
        return PetsFortJrpcClient.get(applicationContext);
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


        public static void subscribeBasicTopics(@NonNull String userId, @NonNull String role, boolean on_order, final OnCompleteListener<Void> listener) {
            // Define basic topics based on user details. Customize topic names as needed.
            final String userTopic = "user_" + userId;      // e.g., user_12345
            final String roleTopic = "role_" + role.toLowerCase(); // e.g., role_admin, role_user
            final String onOrderTopic = "order_checkout"; // e.g., role_admin, role_user
            final String allUsersTopic = "all_users";       // General topic for all users

            // Subscribe to each basic topic.
            // We pass null for the listener here, meaning the caller of subscribeBasicTopics
            // won't be directly notified of individual subscription successes/failures.
            // The fcmTopics map will be updated asynchronously in the subscribeToTopic callbacks.
            if(on_order) {
                subscribeToTopic(onOrderTopic, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
            }
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

        public static void updateCartProduct(SharedPreferences localDB, String userId, String productID, HashMap<String,Object> data) {
            String cartKey = "carts_" + userId;
            HashMap<String,Object> details = getHashMap(localDB);
            HashMap<String,Object> carts = null;
            try {
                LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get(cartKey);
                if(temp == null) {
                    temp = new LinkedTreeMap<>();
                }
                carts = new HashMap<String,Object>(temp);
            } catch (Exception e) {
                carts = new HashMap<>();
            }

            carts.put(productID, data);
            details.put(cartKey,carts);
            saveHashMap(localDB, details);
        }

        public static void  deleteCartProduct(SharedPreferences localDB, String userId, String productID) {
            String cartKey = "carts_" + userId;
            HashMap<String,Object> details = getHashMap(localDB);

            if(details.containsKey(cartKey)) {
                HashMap<String,Object> carts = null;
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get(cartKey);
                    if(temp == null) {
                        temp = new LinkedTreeMap<>();
                    }
                    carts = new HashMap<String,Object>(temp);
                } catch (Exception e) {
                    carts = new HashMap<>();
                }

                if(carts.containsKey(productID)) {
                    carts.remove(productID);
                    details.put(cartKey,carts);
                    saveHashMap(localDB, details);
                }
            }
        }

        public static HashMap<String,Object> getCartProduct(SharedPreferences localDB, String userId, String productID) {
            String cartKey = "carts_" + userId;
            HashMap<String,Object> details = getHashMap(localDB);

            if(details.containsKey(cartKey)) {
                HashMap<String,Object> carts = null;
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get(cartKey);
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

        public static HashMap<String,Object> getCart(SharedPreferences localDB, String userId) {
            String cartKey = "carts_" + userId;
            HashMap<String,Object> details = getHashMap(localDB);

            HashMap<String,Object> carts = new HashMap<>();
            if(details.containsKey(cartKey)) {
                try {
                    LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String, Object>) details.get(cartKey);
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

        public static void clearCart(SharedPreferences localDB, String userId) {
            String cartKey = "carts_" + userId;
            HashMap<String,Object> details = getHashMap(localDB);
            if(details.containsKey(cartKey)) {
                details.remove(cartKey);
                saveHashMap(localDB, details);
            }
        }
    }


    public static class UserDataApiClient {
//        private static final OkHttpClient client = new OkHttpClient();

        public static void putUserDataCallApi(String userId, User user, Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            HashMap<String,Object> dataMap = new HashMap<>();
            dataMap.put("name",user.name);
            dataMap.put("contact",user.contact);
            dataMap.put("gstin",user.gstin);
            dataMap.put("email",user.email);
            dataMap.put("role",user.role);
            dataMap.put("address",user.address);
            dataMap.put("creditse",user.creditse);
            dataMap.put("credits",user.credits);
            dataMap.put("isblocked",user.isBlocked);
            dataMap.put("pwd","");
            com.google.gson.JsonObject request = PetsFortJrpcClient.requestWithBody(dataMap);
            request.addProperty("user_id", userId);
            rpcClient().call(CrmRpc.PUT_USERDATA, request, new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    callback.onReceived(new UserDataApiResponse(200, user));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new UserDataApiResponse(statusCode, (User) null));
                }
            });
        }
        public static void getUserDataCallApi(String userId, Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            com.google.gson.JsonObject request = new com.google.gson.JsonObject();
            request.addProperty("user_id", userId);
            rpcClient().call(CrmRpc.GET_USER, request, new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    callback.onReceived(new UserDataApiResponse(200, parseUser(response.toString())));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new UserDataApiResponse(statusCode, (User) null));
                }
            });
        }

        public static void getAllUsersCallApi(Callbacker.ApiResponseWaiters.UserDataApiCallback callback) {
            rpcClient().call(CrmRpc.GET_USERDATA, new com.google.gson.JsonObject(), new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    String data = response.has("data") ? response.get("data").toString() : "[]";
                    callback.onReceived(new UserDataApiResponse(200, parseUsers(data)));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new UserDataApiResponse(statusCode, (List<User>) null));
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
                String contact = obj.getString("contact");
                String gstin = obj.getString("gstin");
                String email = obj.getString("email");
                String role = obj.getString("role");
                String address = obj.getString("address");
                String creditse = obj.getString("creditse");
                double credits = obj.getDouble("credits");
                int isBlocked = obj.getInt("isblocked");

                return new User(uid, id, name, contact, gstin, email, role, address,
                        credits, creditse, isBlocked);
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
//        private static final OkHttpClient client = new OkHttpClient();

        public static void getCategoriesCallApi(Callbacker.ApiResponseWaiters.CategoriesApiCallback callback) {
            rpcClient().call(CrmRpc.GET_CATEGORIES, new com.google.gson.JsonObject(), new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    String data = response.has("data") ? response.get("data").toString() : "[]";
                    callback.onReceived(new CategoriesApiResponse(200, parseCategories(data)));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new CategoriesApiResponse(statusCode, new ArrayList<>()));
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

    public static class SubCategoriesApiClient {
//        private static final OkHttpClient client = new OkHttpClient();

        public static void getSubCategoriesCallApi(String category_id, Callbacker.ApiResponseWaiters.SubCategoriesApiCallback callback) {
            com.google.gson.JsonObject request = new com.google.gson.JsonObject();
            request.addProperty("category_id", category_id);
            rpcClient().call(CrmRpc.GET_SUBCATEGORIES_BY_CATEGORY, request, new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    String data = response.has("data") ? response.get("data").toString() : "[]";
                    callback.onReceived(new SubCategoriesApiResponse(200, parseCategories(data)));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new SubCategoriesApiResponse(statusCode, new ArrayList<>()));
                }
            });
        }

        public static ArrayList<SubCategory> parseCategories(String responseBody) {
            ArrayList<SubCategory> categories = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(responseBody);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String parentid = obj.getString("parentid");
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String image = obj.getString("image");

                    SubCategory category = new SubCategory(parentid,id,name,image);
                    categories.add(category);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return categories;
        }

        public static class SubCategoriesApiResponse {
            private final int statusCode;
            private final ArrayList<SubCategory> categories;

            public SubCategoriesApiResponse(int statusCode, ArrayList<SubCategory> categories) {
                this.statusCode = statusCode;
                this.categories = categories;
            }

            public ArrayList<SubCategory> getSubCategories() {return categories;}
            public int getStatusCode() {
                return statusCode;
            }
        }

    }

    public static class QueryApiClient {
//        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, Callbacker.ApiResponseWaiters.QueryApiCallback callback) {
            rpcClient().call(CrmRpc.POST_PRODUCTS_QUERY, PetsFortJrpcClient.requestWithBody(data),
                    new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    try {
                        String result = response.has("data") ? response.get("data").toString() : "[]";
                        callback.onReceived(new QueryApiResponse(200, parseProducts(result)));
                    } catch (JSONException error) {
                        callback.onReceived(new QueryApiResponse(500, new ArrayList<>()));
                    }
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new QueryApiResponse(statusCode, new ArrayList<>()));
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
                product.setProductCid(jsonObject.optString("product_cid"));
                product.setProductHsn(jsonObject.optString("product_hsn"));
                product.setProductDesc(jsonObject.optString("product_desc"));
                product.setProductImg(parseJsonArrayToList(jsonObject.optJSONArray("product_img")));
                product.setCatId(jsonObject.optString("cat_id"));
                product.setCatSub(jsonObject.optString("cat_sub"));
                product.setCostRate(jsonObject.optDouble("cost_rate"));
                product.setCostMrp(jsonObject.optDouble("cost_mrp"));
                product.setCostGst(jsonObject.optDouble("cost_gst"));
                product.setCostDis(jsonObject.optDouble("cost_dis"));
                product.setOfferBuyQty(jsonObject.optInt("offer_buy_qty", 0));
                product.setOfferFreeQty(jsonObject.optInt("offer_free_qty", 0));
                product.setOfferActive(jsonObject.optBoolean("offer_active", false));
                product.setOfferGroupId(jsonObject.optString("offer_group_id", ""));
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
//        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, Callbacker.ApiResponseWaiters.BulkDetailsApiCallback callback) {
            rpcClient().call(CrmRpc.POST_PRODUCTS_BULK_DETAILS, PetsFortJrpcClient.requestWithBody(data),
                    new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    try {
                        callback.onReceived(new BulkDetailsApiResponse(200, parseResponse(response.toString())));
                    } catch (JSONException error) {
                        callback.onReceived(new BulkDetailsApiResponse(500, new BulkDetailsApiResponse()));
                    }
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new BulkDetailsApiResponse(statusCode, new BulkDetailsApiResponse()));
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
                    product.setProductCid(productJson.optString("product_cid"));
                    product.setProductHsn(productJson.optString("product_hsn"));
                    product.setProductDesc(productJson.optString("product_desc"));
                    product.setProductImg(parseJsonArrayToList(productJson.optJSONArray("product_img")));
                    product.setCatId(productJson.optString("cat_id"));
                    product.setCatSub(productJson.optString("cat_sub"));
                    product.setCostRate(productJson.optDouble("cost_rate"));
                    product.setCostMrp(productJson.optDouble("cost_mrp"));
                    product.setCostGst(productJson.optDouble("cost_gst"));
                    product.setCostDis(productJson.optDouble("cost_dis"));
                    product.setOfferBuyQty(productJson.optInt("offer_buy_qty", 0));
                    product.setOfferFreeQty(productJson.optInt("offer_free_qty", 0));
                    product.setOfferActive(productJson.optBoolean("offer_active", false));
                    product.setOfferGroupId(productJson.optString("offer_group_id", ""));
                    product.setStock(productJson.optInt("stock"));
                    product.setId(productJson.optString("id"));
//                    product.setCreatedAt(productJson.optString("created_at"));
//                    product.setUpdatedAt(productJson.optString("updated_at"));
                    products.add(product);
                }
            }

            CostDetails costDetails = new CostDetails(
                    costObject != null ? costObject.optDouble("total_mrp", 0.0) : 0.0,
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
                this.costDetails = new CostDetails(0.0,0.0, 0.0, 0.0, 0.0);
            }

            public ArrayList<Product> getProducts() { return products; }
            public CostDetails getCostDetails() { return costDetails; }
            public int getStatusCode() { return statusCode; }
        }

        public static class CostDetails {
            private final double totalMrp;
            private final double totalRate;
            private final double totalGst;
            private final double total;
            private final double totalDiscount;

            public CostDetails(double totalMrp, double totalRate, double totalGst, double total, double totalDiscount) {
                this.totalMrp = totalMrp;
                this.totalRate = totalRate;
                this.totalGst = totalGst;
                this.total = total;
                this.totalDiscount = totalDiscount;
            }

            public double getTotalMrp() { return totalMrp; }
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

//        private final OkHttpClient client = new OkHttpClient();

        /**
         * Calls the order checkout API.
         * The API now returns a simple status ("stored" or "failed").
         *
         * @param userId   The ID of the user placing the order.
         * @param data     A HashMap representing the order payload, e.g., {"product_id1": {"count": 2}}
         * @param callback The callback to handle the API response.
         */
        public void callApi(String userId, HashMap<String, Object> data, Callbacker.ApiResponseWaiters.OrderCheckoutApiCallback callback) {
            com.google.gson.JsonObject request = PetsFortJrpcClient.requestWithBody(data);
            request.addProperty("user_id", userId);
            rpcClient().call(CrmRpc.POST_ORDERS_CHECKOUT, request, new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    String orderId = response.has("order_id") ? response.get("order_id").getAsString() : "";
                    String status = response.has("order_status") ? response.get("order_status").getAsString() : null;
                    String error = null;
                    int statusCode = 200;
                    if (response.has("message") && "OutOfStock".equals(response.get("message").getAsString())) {
                        error = "OutOfStock,"
                                + (response.has("product_available_stock") ? response.get("product_available_stock").getAsString() : "0") + ","
                                + (response.has("product_id") ? response.get("product_id").getAsString() : "0") + ","
                                + (response.has("product_name") ? response.get("product_name").getAsString() : "product");
                        statusCode = 500;
                    } else if (status == null) {
                        error = "API response missing 'status' field.";
                        statusCode = 500;
                    }
                    callback.onReceived(new OrderCheckoutApiResponse(orderId, statusCode, status, error));
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new OrderCheckoutApiResponse("", statusCode, "failed", message));
                }
            });
        }

        /**
         * Data class to hold the simplified response from the Order Checkout API.
         */
        public class OrderCheckoutApiResponse {
            private final String orderId;
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
            public OrderCheckoutApiResponse(String orderId,int statusCode, String status, String errorMessage) {
                this.orderId = orderId;
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
            public String getOrderId() {
                return orderId;
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
//        private final OkHttpClient client = new OkHttpClient();

        public void callApi(HashMap<String, Object> data, OrderApiCallback callback) {
            rpcClient().call(CrmRpc.POST_ORDERS_QUERY, PetsFortJrpcClient.requestWithBody(data),
                    new PetsFortJrpcClient.Callback() {
                @Override public void onSuccess(com.google.gson.JsonObject response) {
                    try {
                        String result = response.has("data") ? response.get("data").toString() : "[]";
                        callback.onReceived(new OrderQueryApiResponse(200, parseOrders(result)));
                    } catch (JSONException error) {
                        callback.onReceived(new OrderQueryApiResponse(500, new ArrayList<>()));
                    }
                }
                @Override public void onError(int statusCode, String message) {
                    callback.onReceived(new OrderQueryApiResponse(statusCode, new ArrayList<>()));
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
                            countMap.put("count", countJson.has("paid_count") ? countJson.optInt("paid_count") : countJson.optInt("count"));
                            countMap.put("free_count", countJson.optInt("free_count", 0));
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
                        product.setProductCid(jsonObject.optString("product_cid"));
                        product.setProductHsn(jsonObject.optString("product_hsn"));
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
                        product.setOfferBuyQty(itemDetailJson.optInt("offer_buy_qty", 0));
                        product.setOfferFreeQty(itemDetailJson.optInt("offer_free_qty", 0));
                        product.setOfferActive(itemDetailJson.optInt("free_count", 0) > 0);
                        product.setOfferGroupId(itemDetailJson.optString("offer_group_id", ""));
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
                final double mrp = this.totalRate + this.totalDiscount;
                return new BulkDetailsApiClient.CostDetails(mrp, this.totalRate, this.totalGst, this.total, this.totalDiscount);
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
