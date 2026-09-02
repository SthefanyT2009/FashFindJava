package com.fashfind.fashfind.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.fashfind.fashfind.entity.Venta;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Genera el reporte de ventas en PDF (OpenPDF) y Excel (Apache POI),
 * con los mismos indicadores mostrados en /ventas/reporte y los colores
 * de marca de FashFind (rosa #e91e8c, oscuro #1a1a2e).
 */
@Service
public class ReporteVentaService {

    private static final Color ROSA_AWT = new Color(0xE9, 0x1E, 0x8C);
    private static final Color OSCURO_AWT = new Color(0x1A, 0x1A, 0x2E);
    private static final byte[] ROSA_RGB = new byte[]{(byte) 0xE9, (byte) 0x1E, (byte) 0x8C};

    private static final String[] COLUMNAS =
            {"#", "Fecha", "Hora", "Vendedor", "Metodo de Pago", "Unidades", "Total", "Estado"};

    public byte[] generarPdf(List<Venta> ventas, long totalVentas, long unidadesVendidas,
                              long ingresosTotales, long ventasEfectivo, long ventasTransferencia, long ticketPromedio) {
        Document documento = new Document(PageSize.A4.rotate(), 24, 24, 30, 30);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, OSCURO_AWT);
            Font fuenteSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, ROSA_AWT);
            Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 9, OSCURO_AWT);

            documento.add(new Paragraph("FashFind - Reporte de Ventas", fuenteTitulo));

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            documento.add(new Paragraph("Generado el " + fecha, fuenteSub));
            documento.add(Chunk.NEWLINE);

            String[] etiquetas = {"Ventas", "Unidades", "Efectivo", "Transferencia", "Ticket Promedio", "Ingresos"};
            String[] valores = {
                    String.valueOf(totalVentas),
                    String.valueOf(unidadesVendidas),
                    String.valueOf(ventasEfectivo),
                    String.valueOf(ventasTransferencia),
                    "$" + formatearMiles(ticketPromedio),
                    "$" + formatearMiles(ingresosTotales)
            };

            PdfPTable resumen = new PdfPTable(etiquetas.length);
            resumen.setWidthPercentage(100);
            for (int i = 0; i < etiquetas.length; i++) {
                PdfPCell celda = new PdfPCell();
                celda.setBorderColor(new Color(0xF0, 0xDB, 0xE9));
                celda.setPadding(8);
                celda.addElement(new Paragraph(etiquetas[i], fuenteEtiqueta));
                celda.addElement(new Paragraph(valores[i], fuenteValor));
                resumen.addCell(celda);
            }
            documento.add(resumen);
            documento.add(Chunk.NEWLINE);

            PdfPTable tabla = new PdfPTable(COLUMNAS.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{0.6f, 1.1f, 1f, 1.6f, 1.4f, 1f, 1.2f, 1f});
            for (String columna : COLUMNAS) {
                PdfPCell celda = new PdfPCell(new Phrase(columna, fuenteEncabezado));
                celda.setBackgroundColor(ROSA_AWT);
                celda.setPadding(6);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }

            DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");
            for (Venta v : ventas) {
                agregarCelda(tabla, "#" + v.getIdVenta(), fuenteCelda);
                agregarCelda(tabla, v.getFechaVenta().format(formatoFecha), fuenteCelda);
                agregarCelda(tabla, v.getHora().format(formatoHora), fuenteCelda);
                agregarCelda(tabla, v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos(), fuenteCelda);
                agregarCelda(tabla, v.getMetodoPago().toString(), fuenteCelda);
                agregarCelda(tabla, String.valueOf(v.getTotalUnidades()), fuenteCelda);
                agregarCelda(tabla, "$" + formatearMiles(v.getCostoTotal()), fuenteCelda);
                agregarCelda(tabla, v.getEstado(), fuenteCelda);
            }
            documento.add(tabla);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF del reporte de ventas", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    public byte[] generarExcel(List<Venta> ventas, long totalVentas, long unidadesVendidas,
                                long ingresosTotales, long ventasEfectivo, long ventasTransferencia, long ticketPromedio) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");

            XSSFCellStyle estiloTitulo = libro.createCellStyle();
            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            estiloTitulo.setFont(fuenteTitulo);

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.createCell(0).setCellValue("FashFind - Reporte de Ventas");
            filaTitulo.getCell(0).setCellStyle(estiloTitulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, COLUMNAS.length - 1));

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            hoja.createRow(1).createCell(0).setCellValue("Generado el " + fecha);

            String[] etiquetasResumen = {"Ventas", "Unidades", "Efectivo", "Transferencia", "Ticket Promedio", "Ingresos"};
            long[] valoresResumen = {totalVentas, unidadesVendidas, ventasEfectivo, ventasTransferencia, ticketPromedio, ingresosTotales};

            XSSFCellStyle estiloEtiquetaResumen = libro.createCellStyle();
            XSSFFont fuenteEtiquetaResumen = libro.createFont();
            fuenteEtiquetaResumen.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            estiloEtiquetaResumen.setFont(fuenteEtiquetaResumen);

            Row filaEtiquetasResumen = hoja.createRow(3);
            Row filaValoresResumen = hoja.createRow(4);
            for (int i = 0; i < etiquetasResumen.length; i++) {
                filaEtiquetasResumen.createCell(i).setCellValue(etiquetasResumen[i]);
                filaEtiquetasResumen.getCell(i).setCellStyle(estiloEtiquetaResumen);
                filaValoresResumen.createCell(i).setCellValue(valoresResumen[i]);
            }

            XSSFCellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFillForegroundColor(new XSSFColor(ROSA_RGB, null));
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);

            int filaEncabezadoIdx = 6;
            Row filaEncabezado = hoja.createRow(filaEncabezadoIdx);
            for (int i = 0; i < COLUMNAS.length; i++) {
                filaEncabezado.createCell(i).setCellValue(COLUMNAS[i]);
                filaEncabezado.getCell(i).setCellStyle(estiloEncabezado);
            }

            DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");
            int fila = filaEncabezadoIdx + 1;
            for (Venta v : ventas) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue("#" + v.getIdVenta());
                r.createCell(1).setCellValue(v.getFechaVenta().format(formatoFecha));
                r.createCell(2).setCellValue(v.getHora().format(formatoHora));
                r.createCell(3).setCellValue(v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos());
                r.createCell(4).setCellValue(v.getMetodoPago().toString());
                r.createCell(5).setCellValue(v.getTotalUnidades());
                r.createCell(6).setCellValue(v.getCostoTotal());
                r.createCell(7).setCellValue(v.getEstado());
            }

            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.autoSizeColumn(i);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el Excel del reporte de ventas", e);
        }
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "-", fuente));
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private String formatearMiles(long valor) {
        return String.format("%,d", valor).replace(",", ".");
    }
}