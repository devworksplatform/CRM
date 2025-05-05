import os
from bs4 import BeautifulSoup, Tag

# Helper function equivalent to generateSummaryRows
def generate_summary_rows(soup: BeautifulSoup, rows_data: list, col_count_empty: int) -> Tag:
    """Generates table body rows for the summary section."""
    table_body = soup.new_tag('tbody')

    for row_data in rows_data:
        row = soup.new_tag('tr')
        if row_data.get('isBlank', False):
            cell = soup.new_tag('td')
            cell['colspan'] = row_data.get('colspan', 1) # Default colspan to 1 if not provided
            row.append(cell)
        else:
            # Add leading empty cell
            empty_cell_leading = soup.new_tag('td')
            row.append(empty_cell_leading)

            # Label cell
            label_cell = soup.new_tag('td')
            label_cell.string = row_data.get('label', '')
            row.append(label_cell)

            # Intermediate empty cells
            for _ in range(col_count_empty):
                empty_cell_intermediate = soup.new_tag('td')
                row.append(empty_cell_intermediate)

            # Value cell
            value_cell = soup.new_tag('td')
            value_content = row_data.get('value', '')
            if row_data.get('isBold', False):
                bold_element = soup.new_tag('b')
                bold_element.string = str(value_content) # Ensure value is string
                value_cell.append(bold_element)
            else:
                value_cell.string = str(value_content) # Ensure value is string
            row.append(value_cell)

        table_body.append(row)

    return table_body

# Helper function equivalent to generateSummaryRows2
def generate_summary_rows2(soup: BeautifulSoup, rows_data: list, cgst: str, sgst: str) -> Tag:
    """Generates table body rows for the second summary section (GST table)."""
    table_body = soup.new_tag('tbody')

    for row_data in rows_data:
        row = soup.new_tag('tr')
        if row_data.get('isBlank', False):
            cell = soup.new_tag('td')
            cell['colspan'] = row_data.get('colspan', 1) # Default colspan to 1 if not provided
            row.append(cell)
        else:
            # Label cell
            label_cell = soup.new_tag('td')
            label_cell.string = row_data.get('label', '')
            row.append(label_cell)

            # Two empty cells
            for _ in range(2):
                empty_cell_1 = soup.new_tag('td')
                row.append(empty_cell_1)

            # CGST cell (allows HTML via string assignment, but be cautious)
            cgst_cell = soup.new_tag('td')
            cgst_cell.string = str(cgst) # Assign string directly
            row.append(cgst_cell)

            # One empty cell
            empty_cell_2 = soup.new_tag('td')
            row.append(empty_cell_2)

            # SGST cell (allows HTML via string assignment, but be cautious)
            sgst_cell = soup.new_tag('td')
            sgst_cell.string = str(sgst) # Assign string directly
            row.append(sgst_cell)

            # Value cell
            value_cell = soup.new_tag('td')
            value_content = row_data.get('value', '')
            if row_data.get('isBold', False):
                bold_element = soup.new_tag('b')
                bold_element.string = str(value_content) # Ensure value is string
                value_cell.append(bold_element)
            else:
                value_cell.string = str(value_content) # Ensure value is string
            row.append(value_cell)

        table_body.append(row)

    return table_body


# Main function equivalent to generateInvoice (synchronous version)
def generate_invoice(invoice_data: dict, template_path: str = 'bill.html') -> str | None:
    """
    Generates an HTML invoice by populating a template file with data.

    Args:
        invoice_data: A dictionary containing all the invoice details.
        template_path: The path to the HTML template file ('bill.html').

    Returns:
        The generated HTML content as a string, or None if an error occurs.
    """
    try:
        # --- 1. Read the HTML template ---
        if not os.path.exists(template_path):
             print(f"Error: Template file not found at '{template_path}'")
             return None

        with open(template_path, 'r', encoding='utf-8') as f:
            html_template = f.read()

        # --- 2. Parse the HTML ---
        soup = BeautifulSoup(html_template, 'lxml') # Using lxml parser

        # --- 3. Populate data ---

        # Helper function to safely update element text
        def update_element_text(element_id: str, text: str):
            element = soup.find(id=element_id)
            if element:
                element.string = str(text) # Ensure text is string
            # else:
            #     print(f"Warning: Element with ID '{element_id}' not found in template.")

        # Company Information
        update_element_text('Address', invoice_data.get('company', {}).get('address', ''))
        update_element_text('GstNo', invoice_data.get('company', {}).get('gstNo', ''))
        update_element_text('Email', invoice_data.get('company', {}).get('email', ''))

        # Delivery Note Details
        details = invoice_data.get('details', {})
        update_element_text('InvoiceNo', details.get('invoiceNo', ''))
        update_element_text('Dated', details.get('dated', ''))
        update_element_text('DeliveryNote', details.get('deliveryNote', ''))
        update_element_text('RefNoDate', details.get('refNoDate', ''))
        update_element_text('OtherRef', details.get('otherRef', ''))
        update_element_text('CheckedBy', details.get('checkedBy', ''))

        # Consignee and Buyer Details
        update_element_text('Caddress1', invoice_data.get('consignee', {}).get('address', ''))
        update_element_text('Ccontact1', invoice_data.get('consignee', {}).get('contactNo', ''))
        update_element_text('Caddress2', invoice_data.get('buyer', {}).get('address', ''))
        update_element_text('Ccontact2', invoice_data.get('buyer', {}).get('contactNo', ''))

        # Item Details
        product_table_body = soup.find(id='productTableEditable')
        if product_table_body:
            # Clear existing placeholder rows (optional, depends on template)
            product_table_body.clear()

            # Add item rows
            for item in invoice_data.get('items', []):
                row = soup.new_tag('tr')

                # Create and append cells for the item row
                cell_sno = soup.new_tag('td')
                cell_sno.string = str(item.get('sNo', ''))
                row.append(cell_sno)

                cell_desc = soup.new_tag('td')
                cell_desc.string = str(item.get('description', ''))
                row.append(cell_desc)

                cell_hsn = soup.new_tag('td')
                cell_hsn.string = str(item.get('hsnSac', ''))
                row.append(cell_hsn)

                cell_partno = soup.new_tag('td')
                cell_partno.string = str(item.get('partNo', ''))
                row.append(cell_partno)

                cell_qty_shipped = soup.new_tag('td')
                cell_qty_shipped.string = str(item.get('quantityShipped', ''))
                row.append(cell_qty_shipped)

                cell_qty_billed = soup.new_tag('td')
                cell_qty_billed.string = str(item.get('quantityBilled', ''))
                row.append(cell_qty_billed)

                cell_rate = soup.new_tag('td')
                cell_rate.string = f"{item.get('rate', 0.0):.2f}"
                row.append(cell_rate)

                cell_discount = soup.new_tag('td')
                cell_discount.string = str(item.get('discount', '')) # Assuming discount is a string like '0%' or amount
                row.append(cell_discount)

                cell_amount = soup.new_tag('td')
                cell_amount.string = f"{item.get('amount', 0.0):.2f}"
                row.append(cell_amount)

                product_table_body.append(row)

            # Add summary rows using the helper function
            totals = invoice_data.get('totals', {})
            sub_total_value = totals.get('subTotal', 0.0)
            cgst_value = totals.get('cgstAmount', 0.0)
            sgst_value = totals.get('sgstAmount', 0.0)
            discount_value = totals.get('specialDiscount', 0.0)
            round_off_value = totals.get('roundOff', 0.0)
            total_value = totals.get('total', 0.0)

            summary_rows_data = [
                {'colspan': 9, 'isBlank': True},
                {'label': 'Sub Total', 'value': f"{sub_total_value:.2f}"},
                {'label': 'Output CGST', 'value': f"{cgst_value:.2f}"},
                {'label': 'Output SGST', 'value': f"{sgst_value:.2f}"},
                {'label': 'Special Discount', 'value': f"(-){discount_value:.2f}", 'isBold': True},
                {'label': 'Round Off', 'value': f"(-){round_off_value:.2f}", 'isBold': True},
                {'label': 'Total', 'value': f"{total_value:.2f}", 'isBold': True}, # Made Total bold as is common
            ]

            summary_rows_tbody = generate_summary_rows(soup, summary_rows_data, 6) # 6 empty cols between label and value
            product_table_body.append(summary_rows_tbody)
        # else:
            # print("Warning: Element with ID 'productTableEditable' not found.")


        # GST Details
        gst_table_body = soup.find(id='gstTableEditable')
        if gst_table_body:
            # Clear existing placeholder rows (optional)
            gst_table_body.clear()

            # Add GST detail rows
            for gst in invoice_data.get('gstDetails', []):
                row = soup.new_tag('tr')

                # Create and append cells
                cell_hsn = soup.new_tag('td')
                cell_hsn.string = str(gst.get('hsnSac', ''))
                row.append(cell_hsn)

                cell_taxable = soup.new_tag('td')
                cell_taxable.string = f"{gst.get('taxableValue', 0.0):.2f}"
                row.append(cell_taxable)

                cell_cgst_rate = soup.new_tag('td')
                cell_cgst_rate.string = str(gst.get('cgstRate', '')) # Assuming rate is string like '9%'
                row.append(cell_cgst_rate)

                cell_cgst_amount = soup.new_tag('td')
                cell_cgst_amount.string = f"{gst.get('cgstAmount', 0.0):.2f}"
                row.append(cell_cgst_amount)

                cell_sgst_rate = soup.new_tag('td')
                cell_sgst_rate.string = str(gst.get('sgstUtgstRate', '')) # Assuming rate is string like '9%'
                row.append(cell_sgst_rate)

                cell_sgst_amount = soup.new_tag('td')
                cell_sgst_amount.string = f"{gst.get('sgstUtgstAmount', 0.0):.2f}"
                row.append(cell_sgst_amount)

                cell_total_tax = soup.new_tag('td')
                cell_total_tax.string = f"{gst.get('totalTaxAmount', 0.0):.2f}"
                row.append(cell_total_tax)

                gst_table_body.append(row)

            # Add GST summary rows using the second helper function
            totals = invoice_data.get('totals', {})
            total_value = totals.get('total', 0.0)
            cgst_total = totals.get('cgstAmount', 0.0)
            sgst_total = totals.get('sgstAmount', 0.0)

            gst_summary_rows_data = [
                {'colspan': 7, 'isBlank': True}, # Colspan should be 7 for this table structure
                {'label': 'Total', 'value': f"{total_value:.2f}", 'isBold': True}, # Made Total bold
            ]

            summary_rows2_tbody = generate_summary_rows2(soup, gst_summary_rows_data,
                                                       f"{cgst_total:.2f}",
                                                       f"{sgst_total:.2f}")
            gst_table_body.append(summary_rows2_tbody)

        # else:
            # print("Warning: Element with ID 'gstTableEditable' not found.")


        # Amounts in Words
        amounts_words = invoice_data.get('amountsInWords', {})
        update_element_text('taxAmountInWords', amounts_words.get('taxAmount', ''))
        update_element_text('AmountChargeableInWords', amounts_words.get('amountChargeable', ''))


        # --- 4. Return modified HTML ---
        # Use prettify() for readable output or str(soup) for compact output
        return soup.prettify()
        # return str(soup)


    except FileNotFoundError:
        print(f"Error: Template file not found at '{template_path}'")
        return None
    except Exception as e:
        print(f"An error occurred during invoice generation: {e}")
        # import traceback
        # traceback.print_exc() # Uncomment for detailed traceback
        return None




my_invoice_data_python = {
    'company': {
        'address': 'Your Company Address, City, Postal Code',
        'gstNo': 'YOUR_GST_NUMBER',
        'email': 'your.email@example.com'
    },
    'details': {
        'invoiceNo': 'INV-2023-001',
        'dated': '2023-10-27',
        'deliveryNote': 'DN-2023-005',
        'refNoDate': 'REF-12345 / 2023-10-26',
        'otherRef': 'Some other reference',
        'checkedBy': 'John Doe'
    },
    'consignee': {
        'address': 'Consignee Address, City, Postal Code',
        'contactNo': '+1 123 456 7890'
    },
    'buyer': {
        'address': 'Buyer Address, City, Postal Code',
        'contactNo': '+1 987 654 3210'
    },
    'items': [
        # Note: Rate/Amount are floats, Quantity/Discount are strings as per JS example
        {'sNo': 1, 'description': 'Nunbell Milk Bottle', 'hsnSac': '39233090', 'partNo': 'ACO38', 'quantityShipped': '10 No', 'quantityBilled': '10 No', 'rate': 210.00, 'discount': '50 %', 'amount': 1050.00},
        {'sNo': 2, 'description': 'Another Product', 'hsnSac': 'ABCD', 'partNo': 'XYZ', 'quantityShipped': '5 Each', 'quantityBilled': '5 Each', 'rate': 50.00, 'discount': '10 %', 'amount': 225.00},
    ],
    # Totals based on the example data
    'totals': {
        'subTotal': 1275.00,
        'cgstAmount': 114.75,
        'sgstAmount': 114.75,
        'specialDiscount': 50.00,
        'roundOff': 0.00,
        'total': 1454.50 # 1275 + 114.75 + 114.75 - 50 = 1454.50
    },
    'gstDetails': [
        {'hsnSac': '39233090', 'taxableValue': 1050.00, 'cgstRate': '9%', 'cgstAmount': 94.50, 'sgstUtgstRate': '9%', 'sgstUtgstAmount': 94.50, 'totalTaxAmount': 189.00},
        {'hsnSac': 'ABCD', 'taxableValue': 225.00, 'cgstRate': '9%', 'cgstAmount': 20.25, 'sgstUtgstRate': '9%', 'sgstUtgstAmount': 20.25, 'totalTaxAmount': 40.50},
    ],
    'amountsInWords': {
        'amountChargeable': 'INR One Thousand Four Hundred Fifty Four and Fifty Paise Only',
        'taxAmount': 'INR Two Hundred Twenty Nine and Fifty Paise Only' # 114.75 + 114.75 = 229.50
    }
}

def start():

    # Call the function
    generated_html_content = generate_invoice(my_invoice_data_python)

    # Check if HTML was generated and process it (e.g., save to file)
    if generated_html_content:
        output_filename = "generated_invoice_from_data.html"
        try:
            with open(output_filename, "w", encoding="utf-8") as f:
                f.write(generated_html_content)
            print(f"Invoice generated successfully and saved to {output_filename}")
        except IOError as e:
            print(f"Error saving the generated invoice file: {e}")
    else:
        print("Invoice generation failed. Check previous error messages.")