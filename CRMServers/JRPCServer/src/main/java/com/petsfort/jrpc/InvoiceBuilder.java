package com.petsfort.jrpc;

import com.google.gson.*;
import java.time.*;
import java.util.*;

final class InvoiceBuilder {
    private InvoiceBuilder() {}

    static JsonObject create(String orderId,String created,String userId,JsonObject user,JsonArray products,
                             JsonObject values,double totalRate,double totalGst,double discount,double total){
        JsonObject invoice=new JsonObject();
        JsonObject company=new JsonObject();company.addProperty("name","Petsfort");
        company.addProperty("address","Your Company Address, City, Postal Code");
        company.addProperty("gstNo","YOUR_GST_NUMBER");company.addProperty("email","petsfort.in@gamil.com");
        invoice.add("company",company);
        JsonObject details=new JsonObject();details.addProperty("invoiceNo","INV-"+orderId);
        details.addProperty("dated",created.substring(0,10));details.addProperty("deliveryNote","DN-"+orderId);
        details.addProperty("refNoDate","REF-"+userId);
        details.addProperty("otherRef",Jsons.optionalString(values,"notes","N/A"));
        details.addProperty("checkedBy","System Generated");invoice.add("details",details);
        JsonObject consignee=new JsonObject();consignee.addProperty("name",Jsons.optionalString(user,"name","N/A"));
        consignee.addProperty("address",Jsons.optionalString(values,"address","N/A"));invoice.add("consignee",consignee);
        JsonObject buyer=new JsonObject();buyer.addProperty("name",Jsons.optionalString(user,"name","N/A"));
        buyer.addProperty("address",Jsons.optionalString(user,"address","N/A"));
        buyer.addProperty("contactNo",Jsons.optionalString(user,"contact","N/A"));
        JsonElement gstin=user.get("gstin");
        buyer.add("gstin",gstin==null?JsonNull.INSTANCE:gstin.deepCopy());invoice.add("buyer",buyer);
        JsonArray items=new JsonArray();LinkedHashMap<String,JsonObject> summaries=new LinkedHashMap<>();
        double sub=0,cgst=0,sgst=0;int sequence=1;
        for(JsonElement value:products){JsonObject p=value.getAsJsonObject();
            double mrp=num(p,"cost_mrp"),rate=num(p,"cost_rate"),gst=num(p,"cost_gst"),dis=num(p,"cost_dis");
            int shipped=integer(p,"count"),paid=integer(p,"paid_count"),free=integer(p,"free_count");
            String cid=Jsons.optionalString(p,"product_cid","N/A"),hsn=Jsons.optionalString(p,"product_hsn","N/A");
            double taxable=rate*paid,ca=rate*(gst/2)/100*paid,sa=ca;
            JsonObject item=new JsonObject();item.addProperty("sNo",sequence++);
            item.addProperty("description",Jsons.optionalString(p,"product_name","N/A")+(free>0?" ("+free+" free)":""));
            item.addProperty("hsnSac",hsn);item.addProperty("partNo",cid);
            item.addProperty("quantityShipped",shipped+" No");item.addProperty("quantityBilled",paid+" No");
            item.addProperty("mrp",Jsons.round2(mrp));item.addProperty("discount",dis+" %");
            item.addProperty("rate",Jsons.round2(rate));item.addProperty("amount",Jsons.round2(taxable));items.add(item);
            JsonObject summary=summaries.computeIfAbsent(cid,k->{JsonObject o=new JsonObject();
                o.addProperty("hsnSac",k);o.addProperty("taxableValue",0);o.addProperty("cgstRate",(gst/2)+"%");
                o.addProperty("cgstAmount",0);o.addProperty("sgstUtgstRate",(gst/2)+"%");
                o.addProperty("sgstUtgstAmount",0);o.addProperty("totalTaxAmount",0);return o;});
            summary.addProperty("taxableValue",num(summary,"taxableValue")+taxable);
            summary.addProperty("cgstAmount",num(summary,"cgstAmount")+ca);
            summary.addProperty("sgstUtgstAmount",num(summary,"sgstUtgstAmount")+sa);
            summary.addProperty("totalTaxAmount",num(summary,"totalTaxAmount")+ca+sa);
            sub+=taxable;cgst+=ca;sgst+=sa;
        }
        invoice.add("items",items);JsonArray gstDetails=new JsonArray();
        summaries.values().forEach(s->{for(String key:List.of("taxableValue","cgstAmount","sgstUtgstAmount","totalTaxAmount"))
            s.addProperty(key,Jsons.round2(num(s,key)));gstDetails.add(s);});invoice.add("gstDetails",gstDetails);
        double rawGrand=sub+cgst+sgst,grand=Jsons.round2(rawGrand);JsonObject totals=new JsonObject();
        totals.addProperty("subTotal",Jsons.round2(sub));totals.addProperty("cgstAmount",Jsons.round2(cgst));
        totals.addProperty("sgstAmount",Jsons.round2(sgst));totals.addProperty("specialDiscount",0.0);
        totals.addProperty("roundOff",0.0);totals.addProperty("total",grand);invoice.add("totals",totals);
        JsonObject words=new JsonObject();words.addProperty("amountChargeable",amountWords(rawGrand));
        words.addProperty("taxAmount",amountWords(cgst+sgst));invoice.add("amountsInWords",words);return invoice;
    }
    private static double num(JsonObject o,String k){return !o.has(k)||o.get(k).isJsonNull()?0:o.get(k).getAsDouble();}
    private static int integer(JsonObject o,String k){return !o.has(k)||o.get(k).isJsonNull()?0:o.get(k).getAsInt();}
    private static String amountWords(double value){
        long rupees=(long)value;String text=Double.toString(value),fraction=text.contains(".")?text.substring(text.indexOf('.')+1):text;
        if(fraction.length()>2)fraction=fraction.substring(0,2);int paise=fraction.isEmpty()?0:Integer.parseInt(fraction);
        String result="INR "+integerWords(rupees)+" Rupees";
        if(paise>0)result+=" and "+integerWords(paise)+" Paise";return result+" Only";
    }
    private static String integerWords(long n){
        String[] ones={"","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"};
        String[] tens={"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};
        if(n==0)return"Zero";List<String> parts=new ArrayList<>();
        long[] units={10000000,100000,1000,100};String[] names={"Crore","Lakh","Thousand","Hundred"};
        for(int i=0;i<units.length;i++)if(n>=units[i]){parts.add(integerWords(n/units[i])+" "+names[i]);n%=units[i];}
        if(n>0){String word=n<20?ones[(int)n]:tens[(int)n/10]+(n%10==0?"":" "+ones[(int)n%10]);
            parts.add((parts.isEmpty()?"":"and ")+word);}return String.join(" ",parts);
    }
}
