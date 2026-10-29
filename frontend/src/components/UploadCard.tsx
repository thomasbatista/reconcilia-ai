import React, { useRef, useState } from 'react';
import { Upload, FileText, CheckCircle2, ChevronDown, ChevronUp, Table, AlertCircle } from 'lucide-react';
import { ColumnMapping, CsvPreviewDto } from '../types';

interface UploadCardProps {
  title: string;
  subtitle: string;
  badge: string;
  file: File | null;
  preview: CsvPreviewDto | null;
  mapping: ColumnMapping | null;
  onFileSelect: (file: File) => void;
  onMappingChange: (mapping: ColumnMapping) => void;
  isLoadingPreview: boolean;
  error?: string | null;
}

export const UploadCard: React.FC<UploadCardProps> = ({
  title,
  subtitle,
  badge,
  file,
  preview,
  mapping,
  onFileSelect,
  onMappingChange,
  isLoadingPreview,
  error,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showPreviewTable, setShowPreviewTable] = useState(false);
  const [isDragging, setIsDragging] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const dropped = e.dataTransfer.files[0];
      if (dropped.name.endsWith('.csv') || dropped.type.includes('csv') || dropped.type.includes('text')) {
        onFileSelect(dropped);
      }
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      onFileSelect(e.target.files[0]);
    }
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 flex flex-col justify-between transition-all hover:border-slate-300">
      <div>
        {/* Header */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-2">
            <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 uppercase tracking-wide">
              {badge}
            </span>
            <h3 className="font-semibold text-slate-900 text-base">{title}</h3>
          </div>
          {preview && (
            <span className="inline-flex items-center text-xs font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              <CheckCircle2 className="h-3 w-3 mr-1 text-emerald-600" />
              {preview.totalRows} registros
            </span>
          )}
        </div>

        <p className="text-xs text-slate-500 mb-4">{subtitle}</p>

        {/* Dropzone */}
        {!file ? (
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-lg p-6 flex flex-col items-center justify-center text-center cursor-pointer transition ${
              isDragging
                ? 'border-indigo-500 bg-indigo-50/50'
                : 'border-slate-200 hover:border-slate-300 bg-slate-50/50 hover:bg-slate-50'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileInputChange}
              accept=".csv"
              className="hidden"
            />
            <div className="h-10 w-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-600 mb-3 shadow-xs">
              <Upload className="h-5 w-5 text-indigo-600" />
            </div>
            <p className="text-sm font-medium text-slate-800">
              Clique para selecionar ou arraste o CSV
            </p>
            <p className="text-xs text-slate-400 mt-1">Formato CSV (.csv), até 10MB</p>
          </div>
        ) : (
          <div className="space-y-4">
            {/* Selected File Box */}
            <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50 border border-slate-200">
              <div className="flex items-center space-x-3 overflow-hidden">
                <div className="p-2 bg-indigo-100 text-indigo-700 rounded-md">
                  <FileText className="h-5 w-5" />
                </div>
                <div className="overflow-hidden">
                  <p className="text-sm font-medium text-slate-800 truncate">{file.name}</p>
                  <p className="text-xs text-slate-400">{(file.size / 1024).toFixed(1)} KB</p>
                </div>
              </div>
              <button
                onClick={() => {
                  if (fileInputRef.current) fileInputRef.current.value = '';
                  fileInputRef.current?.click();
                }}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium px-2 py-1 rounded hover:bg-indigo-50 transition"
              >
                Trocar arquivo
              </button>
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileInputChange}
                accept=".csv"
                className="hidden"
              />
            </div>

            {isLoadingPreview && (
              <div className="flex items-center justify-center p-4 text-xs text-slate-500">
                <span className="animate-spin mr-2">◌</span> Analisando cabeçalhos e registros...
              </div>
            )}

            {/* Column Mapping Section */}
            {preview && mapping && (
              <div className="pt-2 border-t border-slate-100">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-700">Mapeamento de Colunas</span>
                  <span className="text-[11px] text-emerald-600 font-medium">Auto-mapeado</span>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <label className="block text-slate-500 mb-1">Identificador / ID *</label>
                    <select
                      value={mapping.idColumn}
                      onChange={(e) => onMappingChange({ ...mapping, idColumn: e.target.value })}
                      className="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-slate-800 text-xs focus:ring-1 focus:ring-indigo-500"
                    >
                      {preview.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">Data *</label>
                    <select
                      value={mapping.dateColumn}
                      onChange={(e) => onMappingChange({ ...mapping, dateColumn: e.target.value })}
                      className="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-slate-800 text-xs focus:ring-1 focus:ring-indigo-500"
                    >
                      {preview.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">Valor (R$) *</label>
                    <select
                      value={mapping.amountColumn}
                      onChange={(e) => onMappingChange({ ...mapping, amountColumn: e.target.value })}
                      className="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-slate-800 text-xs focus:ring-1 focus:ring-indigo-500"
                    >
                      {preview.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-slate-500 mb-1">Descrição / Tipo</label>
                    <select
                      value={mapping.descriptionColumn}
                      onChange={(e) => onMappingChange({ ...mapping, descriptionColumn: e.target.value })}
                      className="w-full bg-white border border-slate-200 rounded px-2 py-1.5 text-slate-800 text-xs focus:ring-1 focus:ring-indigo-500"
                    >
                      <option value="">(Nenhuma)</option>
                      {preview.headers.map((h) => (
                        <option key={h} value={h}>
                          {h}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Preview Table Toggle */}
                <div className="mt-3">
                  <button
                    onClick={() => setShowPreviewTable(!showPreviewTable)}
                    className="flex items-center text-xs text-slate-500 hover:text-slate-700 transition"
                  >
                    <Table className="h-3.5 w-3.5 mr-1" />
                    {showPreviewTable ? 'Ocultar prévia dos dados' : 'Ver primeiras 5 linhas'}
                    {showPreviewTable ? (
                      <ChevronUp className="h-3 w-3 ml-1" />
                    ) : (
                      <ChevronDown className="h-3 w-3 ml-1" />
                    )}
                  </button>

                  {showPreviewTable && (
                    <div className="mt-2 border border-slate-200 rounded overflow-x-auto max-h-40 text-[11px]">
                      <table className="min-w-full divide-y divide-slate-200">
                        <thead className="bg-slate-50">
                          <tr>
                            {preview.headers.map((h) => (
                              <th key={h} className="px-2 py-1 text-left font-medium text-slate-600">
                                {h}
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 bg-white">
                          {preview.previewRows.map((row, idx) => (
                            <tr key={idx} className="hover:bg-slate-50">
                              {preview.headers.map((h) => (
                                <td key={h} className="px-2 py-1 text-slate-700 whitespace-nowrap">
                                  {row[h] || '-'}
                                </td>
                              ))}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Error message */}
        {error && (
          <div className="mt-3 p-2.5 rounded-lg bg-rose-50 border border-rose-200 flex items-start space-x-2 text-rose-700 text-xs">
            <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}
      </div>
    </div>
  );
};
