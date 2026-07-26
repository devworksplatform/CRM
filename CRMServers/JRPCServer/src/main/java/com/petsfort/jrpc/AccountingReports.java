package com.petsfort.jrpc;

import com.google.gson.*;
import java.time.*;
import java.util.*;

final class AccountingReports {
    private AccountingReports(){}

    static String[] fy(JsonObject q){
        String value=Jsons.optionalString(q,"fy",null);int year;
        if(value!=null&&value.matches("\\d{4}-\\d{2}"))year=Integer.parseInt(value.substring(0,4));
        else{LocalDate now=LocalDate.now();year=now.getMonthValue()>=4?now.getYear():now.getYear()-1;}
        return new String[]{year+"-04-01T00:00:00",(year+1)+"-03-31T23:59:59"};
    }
    static JsonObject bill(JsonElement raw){
        JsonElement parsed=Jsons.parseOr(raw,new JsonObject());return parsed.isJsonObject()?parsed.getAsJsonObject():null;
    }
    static double n(JsonObject o,String k){return o==null||!o.has(k)||o.get(k).isJsonNull()?0:o.get(k).getAsDouble();}
    static String s(JsonObject o,String k){return o==null||!o.has(k)||o.get(k).isJsonNull()?"":o.get(k).getAsString();}

    static void dashboard(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String[] dates=fy(q);JsonArray rows=db.selectArray("SELECT o.order_id,o.created_at,o.total,o.order_status,"+
                "b.bill,u.name user_name FROM orders o LEFT JOIN bills b ON o.order_id=b.order_id "+
                "LEFT JOIN userdata u ON o.user_id=u.uid WHERE o.created_at>=? AND o.created_at<=? ORDER BY o.created_at",
                List.of(dates[0],dates[1]));
        double taxable=0,cgst=0,sgst=0,invoiced=0;int count=0;JsonObject monthly=new JsonObject();
        for(JsonElement e:rows){JsonObject row=e.getAsJsonObject(),b=bill(row.get("bill"));if(b==null)continue;
            JsonObject totals=b.getAsJsonObject("totals");double sub=n(totals,"subTotal"),c=n(totals,"cgstAmount"),
                    sg=n(totals,"sgstAmount"),total=n(totals,"total");taxable+=sub;cgst+=c;sgst+=sg;invoiced+=total;count++;
            String month=s(row,"created_at");if(month.length()>=7){month=month.substring(0,7);
                JsonObject m=monthly.has(month)?monthly.getAsJsonObject(month):new JsonObject();
                m.addProperty("taxable",n(m,"taxable")+sub);m.addProperty("cgst",n(m,"cgst")+c);
                m.addProperty("sgst",n(m,"sgst")+sg);m.addProperty("total",n(m,"total")+total);
                m.addProperty("count",m.has("count")?m.get("count").getAsInt()+1:1);monthly.add(month,m);}}
        JsonArray cn=db.selectArray("SELECT COUNT(*) count,COALESCE(SUM(total),0) total,"+
                "COALESCE(SUM(cgst_total),0) cgst,COALESCE(SUM(sgst_total),0) sgst FROM credit_notes "+
                "WHERE created_at>=? AND created_at<=?",List.of(dates[0],dates[1]));
        JsonArray dn=db.selectArray("SELECT COUNT(*) count,COALESCE(SUM(total),0) total,"+
                "COALESCE(SUM(cgst_total),0) cgst,COALESCE(SUM(sgst_total),0) sgst FROM debit_notes "+
                "WHERE created_at>=? AND created_at<=?",List.of(dates[0],dates[1]));
        JsonObject c=cn.get(0).getAsJsonObject(),d=dn.get(0).getAsJsonObject();
        out.addProperty("fy",Jsons.optionalString(q,"fy","current"));out.addProperty("fy_start",dates[0].substring(0,10));
        out.addProperty("fy_end",dates[1].substring(0,10));out.addProperty("total_taxable",Jsons.round2(taxable));
        out.addProperty("total_cgst",Jsons.round2(cgst));out.addProperty("total_sgst",Jsons.round2(sgst));
        out.addProperty("total_tax",Jsons.round2(cgst+sgst));out.addProperty("total_invoiced",Jsons.round2(invoiced));
        out.addProperty("invoice_count",count);out.addProperty("credit_note_count",c.get("count").getAsLong());
        out.addProperty("credit_note_total",Jsons.round2(n(c,"total")));out.addProperty("debit_note_count",d.get("count").getAsLong());
        out.addProperty("debit_note_total",Jsons.round2(n(d,"total")));
        out.addProperty("net_tax_liability",Jsons.round2(cgst+sgst-n(c,"cgst")-n(c,"sgst")+n(d,"cgst")+n(d,"sgst")));
        out.add("monthly",monthly);
    }

    static JsonArray salesRegister(CrmService db,JsonObject q)throws Exception{
        String from=Jsons.optionalString(q,"from_date",null),to=Jsons.optionalString(q,"to_date",null);
        if(from==null||to==null)throw new ApiFailure(400,"from_date and to_date are required");
        JsonArray rows=db.selectArray("SELECT o.order_id,o.user_id,o.created_at,o.total,o.order_status,b.bill,"+
                "u.name user_name,u.gstin user_gstin FROM orders o LEFT JOIN bills b ON o.order_id=b.order_id "+
                "LEFT JOIN userdata u ON o.user_id=u.uid WHERE o.created_at>=? AND o.created_at<=? ORDER BY o.created_at DESC",
                List.of(from+"T00:00:00",to+"T23:59:59"));JsonArray result=new JsonArray();
        for(JsonElement e:rows){JsonObject row=e.getAsJsonObject(),entry=new JsonObject(),b=bill(row.get("bill"));
            entry.addProperty("order_id",s(row,"order_id"));entry.addProperty("date",s(row,"created_at"));
            entry.addProperty("invoice_no","");entry.addProperty("party_name",s(row,"user_name"));
            entry.addProperty("gstin",s(row,"user_gstin"));entry.addProperty("taxable_value",0);
            entry.addProperty("cgst",0);entry.addProperty("sgst",0);entry.addProperty("total",n(row,"total"));
            entry.addProperty("order_status",s(row,"order_status"));entry.add("items",new JsonArray());entry.add("gst_details",new JsonArray());
            if(b!=null){JsonObject totals=b.getAsJsonObject("totals"),details=b.getAsJsonObject("details"),buyer=b.getAsJsonObject("buyer");
                entry.addProperty("invoice_no",s(details,"invoiceNo"));entry.addProperty("taxable_value",n(totals,"subTotal"));
                entry.addProperty("cgst",n(totals,"cgstAmount"));entry.addProperty("sgst",n(totals,"sgstAmount"));
                entry.addProperty("total",n(totals,"total"));entry.add("items",b.get("items"));entry.add("gst_details",b.get("gstDetails"));
                if(!s(buyer,"gstin").isEmpty())entry.addProperty("gstin",s(buyer,"gstin"));}
            result.add(entry);}return result;
    }

    static void stockSummary(CrmService db,JsonObject out)throws Exception{
        JsonArray rows=db.selectArray("SELECT p.id,p.product_id,p.product_name,p.product_hsn,p.product_cid,"+
                "p.cost_mrp,p.cost_rate,p.cost_gst,p.cost_dis,p.stock,c.name category_name FROM products p "+
                "LEFT JOIN category c ON p.cat_id=c.id ORDER BY p.product_name",List.of());
        JsonArray products=new JsonArray();JsonObject cats=new JsonObject();double svt=0,mvt=0;int items=0,oos=0,low=0;
        for(JsonElement e:rows){JsonObject x=e.getAsJsonObject(),p=new JsonObject();int stock=x.get("stock").getAsInt();
            double mrp=n(x,"cost_mrp"),rate=mrp-mrp*n(x,"cost_dis")/100,sv=rate*stock,mv=mrp*stock;
            String cat=s(x,"category_name");if(cat.isEmpty())cat="Uncategorized";
            p.addProperty("id",s(x,"id"));p.addProperty("product_id",s(x,"product_id"));p.addProperty("name",s(x,"product_name"));
            p.addProperty("hsn",s(x,"product_hsn"));p.addProperty("category",cat);p.addProperty("mrp",Jsons.round2(mrp));
            p.addProperty("rate",Jsons.round2(rate));p.addProperty("gst_pct",n(x,"cost_gst"));p.addProperty("discount_pct",n(x,"cost_dis"));
            p.addProperty("stock",stock);p.addProperty("stock_value",Jsons.round2(sv));p.addProperty("mrp_value",Jsons.round2(mv));products.add(p);
            svt+=sv;mvt+=mv;items+=stock;if(stock==0)oos++;else if(stock<5)low++;
            JsonObject cs=cats.has(cat)?cats.getAsJsonObject(cat):new JsonObject();cs.addProperty("count",cs.has("count")?cs.get("count").getAsInt()+1:1);
            cs.addProperty("stock",(cs.has("stock")?cs.get("stock").getAsInt():0)+stock);cs.addProperty("value",Jsons.round2(n(cs,"value")+sv));cats.add(cat,cs);}
        JsonObject sum=new JsonObject();sum.addProperty("total_products",products.size());sum.addProperty("total_items",items);
        sum.addProperty("total_stock_value",Jsons.round2(svt));sum.addProperty("total_mrp_value",Jsons.round2(mvt));
        sum.addProperty("out_of_stock",oos);sum.addProperty("low_stock",low);out.add("products",products);out.add("summary",sum);out.add("category_summary",cats);
    }

    static void outstanding(CrmService db,JsonObject out)throws Exception{
        JsonArray rows=db.selectArray("SELECT u.uid,u.name,u.email,u.contact,u.gstin,u.credits,COUNT(o.order_id) total_orders,"+
                "COALESCE(SUM(CASE WHEN o.order_status!='ORDER_CANCELLED' THEN o.total ELSE 0 END),0) total_purchased,"+
                "MAX(o.created_at) last_order_date FROM userdata u LEFT JOIN orders o ON u.uid=o.user_id GROUP BY u.uid ORDER BY u.credits DESC",List.of());
        double credits=0,due=0;int withCredit=0,withDue=0;
        for(JsonElement e:rows){JsonObject r=e.getAsJsonObject();double v=n(r,"credits");r.addProperty("credits",Jsons.round2(v));
            r.addProperty("total_purchased",Jsons.round2(n(r,"total_purchased")));
            if(!r.has("last_order_date")||r.get("last_order_date").isJsonNull())r.add("last_order",JsonNull.INSTANCE);
            else r.addProperty("last_order",r.get("last_order_date").getAsString());
            r.remove("last_order_date");credits+=v;if(v<0){due+=-v;withDue++;}else if(v>0)withCredit++;}
        JsonObject sum=new JsonObject();sum.addProperty("total_parties",rows.size());sum.addProperty("total_credits",Jsons.round2(credits));
        sum.addProperty("total_outstanding",Jsons.round2(due));sum.addProperty("parties_with_credit",withCredit);
        sum.addProperty("parties_with_dues",withDue);out.add("parties",rows);out.add("summary",sum);
    }

    static void dashboardExtras(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String[] d=fy(q);out.add("top_customers",db.selectArray("SELECT u.name,u.uid,COUNT(o.order_id) orders,"+
                "ROUND(SUM(CASE WHEN o.order_status!='ORDER_CANCELLED' THEN o.total ELSE 0 END),2) value FROM orders o "+
                "JOIN userdata u ON o.user_id=u.uid WHERE o.created_at>=? AND o.created_at<=? GROUP BY u.uid ORDER BY value DESC LIMIT 10",
                List.of(d[0],d[1])));JsonObject dist=new JsonObject();
        db.selectArray("SELECT order_status,COUNT(*) count,ROUND(COALESCE(SUM(total),0),2) value FROM orders "+
                "WHERE created_at>=? AND created_at<=? GROUP BY order_status",List.of(d[0],d[1])).forEach(e->{
            JsonObject row=e.getAsJsonObject(),v=new JsonObject();v.add("count",row.get("count"));v.add("value",row.get("value"));
            dist.add(s(row,"order_status"),v);});out.add("status_distribution",dist);
        out.add("recent_activity",db.selectArray("SELECT o.order_id,o.created_at date,ROUND(o.total,2) total,"+
                "o.order_status status,u.name party FROM orders o LEFT JOIN userdata u ON o.user_id=u.uid "+
                "WHERE o.created_at>=? AND o.created_at<=? ORDER BY o.created_at DESC LIMIT 10",List.of(d[0],d[1])));
    }

    static void partyLedger(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String from=Jsons.optionalString(q,"from_date",null),to=Jsons.optionalString(q,"to_date",null);
        if(from==null||to==null)throw new ApiFailure(400,"from_date and to_date are required");
        String user=Jsons.optionalString(q,"user_id",null),start=from+"T00:00:00",end=to+"T23:59:59";
        List<Object> params=user==null?List.of(start,end):List.of(user,start,end);
        String condition=user==null?"":"o.user_id=? AND ";
        JsonArray entries=new JsonArray(),orders=db.selectArray("SELECT o.order_id,o.user_id,o.created_at,o.total,b.bill,"+
                "u.name user_name FROM orders o LEFT JOIN bills b ON o.order_id=b.order_id LEFT JOIN userdata u "+
                "ON o.user_id=u.uid WHERE "+condition+"o.created_at>=? AND o.created_at<=? ORDER BY o.created_at",params);
        for(JsonElement e:orders){JsonObject x=e.getAsJsonObject(),v=new JsonObject(),b=bill(x.get("bill"));
            String invoice=b==null?"":s(b.getAsJsonObject("details"),"invoiceNo");
            v.addProperty("date",s(x,"created_at"));v.addProperty("type","Sale");
            v.addProperty("voucher_no",invoice.isEmpty()?"INV-"+s(x,"order_id"):invoice);
            if(!x.has("user_name")||x.get("user_name").isJsonNull())v.add("party_name",JsonNull.INSTANCE);
            else v.add("party_name",x.get("user_name"));v.addProperty("debit",Jsons.round2(n(x,"total")));
            v.addProperty("credit",0);entries.add(v);}
        addNotes(db,entries,true,user,start,end);addNotes(db,entries,false,user,start,end);
        sortEntries(entries);double debit=0,credit=0;for(JsonElement e:entries){debit+=n(e.getAsJsonObject(),"debit");credit+=n(e.getAsJsonObject(),"credit");}
        out.add("entries",entries);out.addProperty("total_debit",Jsons.round2(debit));
        out.addProperty("total_credit",Jsons.round2(credit));out.addProperty("net_balance",Jsons.round2(debit-credit));
    }
    private static void addNotes(CrmService db,JsonArray entries,boolean credit,String user,String start,String end)throws Exception{
        String kind=credit?"credit_notes":"debit_notes",prefix=credit?"cn":"dn";
        List<Object> params=user==null?List.of(start,end):List.of(user,start,end);
        JsonArray notes=db.selectArray("SELECT * FROM "+kind+" WHERE "+(user==null?"":"user_id=? AND ")+
                "created_at>=? AND created_at<=? ORDER BY created_at",params);
        for(JsonElement e:notes){JsonObject x=e.getAsJsonObject(),v=new JsonObject();v.addProperty("date",s(x,"created_at"));
            v.addProperty("type",credit?"Credit Note":"Debit Note");v.addProperty("voucher_no",s(x,prefix+"_number"));
            v.addProperty("party_name",s(x,"user_name"));v.addProperty("debit",credit?0:Jsons.round2(n(x,"total")));
            v.addProperty("credit",credit?Jsons.round2(n(x,"total")):0);entries.add(v);}
    }
    private static void sortEntries(JsonArray array){
        List<JsonElement> values=new ArrayList<>();array.forEach(values::add);
        values.sort(Comparator.comparing(v->s(v.getAsJsonObject(),"date")));while(array.size()>0)array.remove(0);values.forEach(array::add);
    }
    static void dayBook(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String from=Jsons.optionalString(q,"from_date",null),to=Jsons.optionalString(q,"to_date",null);
        if(from==null||to==null)throw new ApiFailure(400,"from_date and to_date required");
        String start=from+"T00:00:00",end=to+"T23:59:59";JsonArray entries=new JsonArray();
        JsonArray orders=db.selectArray("SELECT o.order_id,o.user_id,o.created_at,o.total,o.total_rate,o.total_gst,"+
                "o.total_discount,o.order_status,b.bill,u.name user_name FROM orders o LEFT JOIN bills b "+
                "ON o.order_id=b.order_id LEFT JOIN userdata u ON o.user_id=u.uid WHERE o.created_at>=? "+
                "AND o.created_at<=? ORDER BY o.created_at",List.of(start,end));
        for(JsonElement e:orders){JsonObject x=e.getAsJsonObject(),v=new JsonObject(),b=bill(x.get("bill"));
            String invoice=b==null?"":s(b.getAsJsonObject("details"),"invoiceNo");v.addProperty("date",s(x,"created_at"));
            v.addProperty("type","Sales");v.addProperty("voucher_no",invoice.isEmpty()?"INV-"+s(x,"order_id"):invoice);
            String party=s(x,"user_name");if(party.isEmpty())party=s(x,"user_id");v.addProperty("party",party);
            v.addProperty("amount",Jsons.round2(n(x,"total")));v.addProperty("taxable",Jsons.round2(n(x,"total_rate")));
            v.addProperty("tax",Jsons.round2(n(x,"total_gst")));v.addProperty("status",s(x,"order_status"));
            v.addProperty("order_id",s(x,"order_id"));entries.add(v);}
        addDayNotes(db,entries,true,start,end);addDayNotes(db,entries,false,start,end);sortEntries(entries);
        double sales=0,cn=0,dn=0;int sc=0,cc=0,dc=0;for(JsonElement e:entries){JsonObject v=e.getAsJsonObject();
            switch(s(v,"type")){case"Sales":sales+=n(v,"amount");sc++;break;case"Credit Note":cn+=n(v,"amount");cc++;break;default:dn+=n(v,"amount");dc++;}}
        JsonObject sum=new JsonObject();sum.addProperty("total_entries",entries.size());sum.addProperty("sales_count",sc);
        sum.addProperty("cn_count",cc);sum.addProperty("dn_count",dc);sum.addProperty("total_sales",Jsons.round2(sales));
        sum.addProperty("total_cn",Jsons.round2(cn));sum.addProperty("total_dn",Jsons.round2(dn));
        sum.addProperty("net_amount",Jsons.round2(sales-cn+dn));out.add("entries",entries);out.add("summary",sum);
    }
    private static void addDayNotes(CrmService db,JsonArray entries,boolean credit,String start,String end)throws Exception{
        String table=credit?"credit_notes":"debit_notes",prefix=credit?"cn":"dn";
        for(JsonElement e:db.selectArray("SELECT * FROM "+table+" WHERE created_at>=? AND created_at<=? ORDER BY created_at",List.of(start,end))){
            JsonObject x=e.getAsJsonObject(),v=new JsonObject();v.addProperty("date",s(x,"created_at"));
            v.addProperty("type",credit?"Credit Note":"Debit Note");v.addProperty("voucher_no",s(x,prefix+"_number"));
            String party=s(x,"user_name");if(party.isEmpty())party=s(x,"user_id");v.addProperty("party",party);
            v.addProperty("amount",Jsons.round2(n(x,"total")));v.addProperty("taxable",Jsons.round2(n(x,"subtotal")));
            v.addProperty("tax",Jsons.round2(n(x,"cgst_total")+n(x,"sgst_total")));v.addProperty("status","Issued");
            v.addProperty("order_id",s(x,"original_invoice"));entries.add(v);}
    }
    static void profitLoss(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String[] d=fy(q);JsonArray rows=db.selectArray("SELECT o.order_id,o.total,o.total_rate,o.total_gst,"+
                "o.total_discount,o.order_status,o.created_at,b.bill FROM orders o LEFT JOIN bills b "+
                "ON o.order_id=b.order_id WHERE o.created_at>=? AND o.created_at<=?",List.of(d[0],d[1]));
        double revenue=0,taxable=0,cgst=0,sgst=0,mrp=0,discount=0,cancelledValue=0;int cancelled=0;JsonObject monthly=new JsonObject();
        for(JsonElement e:rows){JsonObject x=e.getAsJsonObject();if("ORDER_CANCELLED".equals(s(x,"order_status"))){cancelled++;cancelledValue+=n(x,"total");continue;}
            JsonObject b=bill(x.get("bill"));revenue+=n(x,"total");taxable+=n(x,"total_rate");discount+=n(x,"total_discount");
            if(b!=null){JsonObject totals=b.getAsJsonObject("totals");cgst+=n(totals,"cgstAmount");sgst+=n(totals,"sgstAmount");
                JsonArray items=b.has("items")?b.getAsJsonArray("items"):new JsonArray();for(JsonElement ie:items){JsonObject item=ie.getAsJsonObject();
                    String quantity=s(item,"quantityBilled").replaceAll("\\D","");int qty=quantity.isEmpty()?0:Integer.parseInt(quantity);
                    mrp+=n(item,"mrp")*qty;}}else{cgst+=n(x,"total_gst")/2;sgst+=n(x,"total_gst")/2;}
            String month=s(x,"created_at");if(month.length()>=7){month=month.substring(0,7);JsonObject m=monthly.has(month)?monthly.getAsJsonObject(month):new JsonObject();
                m.addProperty("revenue",n(m,"revenue")+n(x,"total"));m.addProperty("taxable",n(m,"taxable")+n(x,"total_rate"));
                m.addProperty("tax",n(m,"tax")+n(x,"total_gst"));m.addProperty("discount",n(m,"discount")+n(x,"total_discount"));
                m.addProperty("count",m.has("count")?m.get("count").getAsInt()+1:1);monthly.add(month,m);}}
        JsonObject cn=db.selectArray("SELECT COALESCE(SUM(total),0) total,COALESCE(SUM(subtotal),0) subtotal,"+
                "COALESCE(SUM(cgst_total),0) cgst,COALESCE(SUM(sgst_total),0) sgst FROM credit_notes "+
                "WHERE created_at>=? AND created_at<=?",List.of(d[0],d[1])).get(0).getAsJsonObject();
        JsonObject dn=db.selectArray("SELECT COALESCE(SUM(total),0) total,COALESCE(SUM(subtotal),0) subtotal,"+
                "COALESCE(SUM(cgst_total),0) cgst,COALESCE(SUM(sgst_total),0) sgst FROM debit_notes "+
                "WHERE created_at>=? AND created_at<=?",List.of(d[0],d[1])).get(0).getAsJsonObject();
        double tax=cgst+sgst;out.addProperty("fy",Jsons.optionalString(q,"fy","current"));JsonObject rev=new JsonObject();
        rev.addProperty("gross_sales_mrp",Jsons.round2(mrp));rev.addProperty("discount_given",Jsons.round2(discount));
        rev.addProperty("net_sales_taxable",Jsons.round2(taxable));rev.addProperty("cgst_collected",Jsons.round2(cgst));
        rev.addProperty("sgst_collected",Jsons.round2(sgst));rev.addProperty("total_tax_collected",Jsons.round2(tax));
        rev.addProperty("total_revenue",Jsons.round2(revenue));rev.addProperty("credit_note_adj",Jsons.round2(n(cn,"total")));
        rev.addProperty("debit_note_adj",Jsons.round2(n(dn,"total")));rev.addProperty("net_revenue",Jsons.round2(revenue-n(cn,"total")+n(dn,"total")));out.add("revenue",rev);
        JsonObject orders=new JsonObject();orders.addProperty("total_orders",rows.size());orders.addProperty("active_orders",rows.size()-cancelled);
        orders.addProperty("cancelled_orders",cancelled);orders.addProperty("cancelled_value",Jsons.round2(cancelledValue));out.add("orders",orders);
        JsonObject profitability=new JsonObject();profitability.addProperty("gross_mrp_value",Jsons.round2(mrp));
        profitability.addProperty("discount_given",Jsons.round2(discount));profitability.addProperty("effective_selling_price",Jsons.round2(taxable));
        profitability.addProperty("margin_on_mrp",mrp>0?Jsons.round2(mrp-taxable):0);
        profitability.addProperty("margin_percentage",mrp>0?Jsons.round2((mrp-taxable)/mrp*100):0);out.add("profitability",profitability);
        JsonObject ts=new JsonObject();ts.addProperty("output_cgst",Jsons.round2(cgst));ts.addProperty("output_sgst",Jsons.round2(sgst));
        ts.addProperty("total_output_tax",Jsons.round2(tax));ts.addProperty("cn_tax_adj",Jsons.round2(n(cn,"cgst")+n(cn,"sgst")));
        ts.addProperty("dn_tax_adj",Jsons.round2(n(dn,"cgst")+n(dn,"sgst")));
        ts.addProperty("net_tax_payable",Jsons.round2(tax-n(cn,"cgst")-n(cn,"sgst")+n(dn,"cgst")+n(dn,"sgst")));
        out.add("tax_summary",ts);out.add("monthly",monthly);
    }
    static void taxLedger(CrmService db,JsonObject q,JsonObject out)throws Exception{
        String from=Jsons.optionalString(q,"from_date",null),to=Jsons.optionalString(q,"to_date",null);
        if(from==null||to==null)throw new ApiFailure(400,"from_date and to_date required");
        JsonArray rows=db.selectArray("SELECT o.order_id,o.created_at,o.total,b.bill,u.name user_name FROM orders o "+
                "LEFT JOIN bills b ON o.order_id=b.order_id LEFT JOIN userdata u ON o.user_id=u.uid "+
                "WHERE o.created_at>=? AND o.created_at<=? ORDER BY o.created_at",List.of(from+"T00:00:00",to+"T23:59:59"));
        JsonObject rates=new JsonObject();JsonArray entries=new JsonArray();
        for(JsonElement e:rows){JsonObject x=e.getAsJsonObject(),b=bill(x.get("bill"));if(b==null)continue;
            JsonArray details=b.has("gstDetails")?b.getAsJsonArray("gstDetails"):new JsonArray();
            for(JsonElement ge:details){JsonObject g=ge.getAsJsonObject();String text=s(g,"cgstRate").replace("%","");
                double rate=(text.isEmpty()?0:Double.parseDouble(text))*2;String key=rate+"%";double tv=n(g,"taxableValue"),
                        ca=n(g,"cgstAmount"),sa=n(g,"sgstUtgstAmount"),tt=n(g,"totalTaxAmount");
                JsonObject sum=rates.has(key)?rates.getAsJsonObject(key):new JsonObject();sum.addProperty("rate",rate);
                sum.addProperty("taxable",n(sum,"taxable")+tv);sum.addProperty("cgst",n(sum,"cgst")+ca);
                sum.addProperty("sgst",n(sum,"sgst")+sa);sum.addProperty("total_tax",n(sum,"total_tax")+tt);
                sum.addProperty("invoices",sum.has("invoices")?sum.get("invoices").getAsInt()+1:1);rates.add(key,sum);
                JsonObject entry=new JsonObject();entry.addProperty("date",s(x,"created_at"));
                JsonObject invoiceDetails=b.getAsJsonObject("details");String invoice=s(invoiceDetails,"invoiceNo");
                entry.addProperty("invoice",invoice.isEmpty()?"INV-"+s(x,"order_id"):invoice);
                if(!x.has("user_name")||x.get("user_name").isJsonNull())entry.add("party",JsonNull.INSTANCE);else entry.add("party",x.get("user_name"));
                entry.addProperty("hsn",s(g,"hsnSac"));entry.addProperty("rate",rate);entry.addProperty("taxable",Jsons.round2(tv));
                entry.addProperty("cgst",Jsons.round2(ca));entry.addProperty("sgst",Jsons.round2(sa));entry.addProperty("total_tax",Jsons.round2(tt));entries.add(entry);}}
        double taxable=0,tax=0;for(Map.Entry<String,JsonElement> e:rates.entrySet()){JsonObject v=e.getValue().getAsJsonObject();
            for(String key:List.of("taxable","cgst","sgst","total_tax"))v.addProperty(key,Jsons.round2(n(v,key)));
            taxable+=n(v,"taxable");tax+=n(v,"total_tax");}
        JsonObject totals=new JsonObject();totals.addProperty("total_taxable",Jsons.round2(taxable));totals.addProperty("total_tax",Jsons.round2(tax));
        out.add("rate_summary",rates);out.add("entries",entries);out.add("totals",totals);
    }
}
