package shopline.com.JLogics;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import shopline.com.JLogics.Models.Product;

public class Business {
    private static final String ServerURL = "http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000";
    Context context;
    public Business(Context context) {
        this.context = context;
    }


    public static class localDB_SharedPref {

        public static String PREF_KEY = "localDB";

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

        public static void deleteCartProduct(SharedPreferences localDB, String productID) {
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

}
