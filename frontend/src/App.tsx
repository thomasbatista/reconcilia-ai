import { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { UploadCard } from './components/UploadCard';
import { ReconciliationControls } from './components/ReconciliationControls';
import { KpiSummary } from './components/KpiSummary';
import { FilterableResultsTable } from './components/FilterableResultsTable';
import { DiscrepancyModal } from './components/DiscrepancyModal';
import { HelpModal } from './components/HelpModal';
import {
  fetchApiStatus,
  previewCsvFile,
  executeReconciliation,
  fetchDemoReconciliation,
  downloadReconciliationCsv,
} from './services/api';
import {
  ApiStatusDto,
  ColumnMapping,
  CsvPreviewDto,
  ReconciliationItemDto,
  ReconciliationResultDto,
} from './types';
import { Sparkles, ShieldCheck } from 'lucide-react';

export function App() {
  const [apiStatus, setApiStatus] = useState<ApiStatusDto | null>(null);

  // File A (ERP) state
  const [fileA, setFileA] = useState<File | null>(null);
  const [previewA, setPreviewA] = useState<CsvPreviewDto | null>(null);
  const [mappingA, setMappingA] = useState<ColumnMapping | null>(null);
  const [loadingPreviewA, setLoadingPreviewA] = useState(false);
  const [errorA, setErrorA] = useState<string | null>(null);

  // File B (Acquirer) state
  const [fileB, setFileB] = useState<File | null>(null);
  const [previewB, setPreviewB] = useState<CsvPreviewDto | null>(null);
  const [mappingB, setMappingB] = useState<ColumnMapping | null>(null);
  const [loadingPreviewB, setLoadingPreviewB] = useState(false);
  const [errorB, setErrorB] = useState<string | null>(null);

  // Tolerances
  const [dateToleranceDays, setDateToleranceDays] = useState(1);
  const [amountTolerance, setAmountTolerance] = useState(0.00);

  // Reconciliation processing
  const [isReconciling, setIsReconciling] = useState(false);
  const [reconciliationResult, setReconciliationResult] = useState<ReconciliationResultDto | null>(null);
  const [generalError, setGeneralError] = useState<string | null>(null);

  // Modal & help
  const [selectedItemForModal, setSelectedItemForModal] = useState<ReconciliationItemDto | null>(null);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isLoadingDemo, setIsLoadingDemo] = useState(false);

  // Load backend status on mount
  useEffect(() => {
    fetchApiStatus()
      .then((status) => setApiStatus(status))
      .catch(() => console.warn('Backend server not yet ready.'));
  }, []);

  // Handlers for File A (ERP)
  const handleFileASelect = async (file: File) => {
    setFileA(file);
    setErrorA(null);
    setLoadingPreviewA(true);
    try {
      const preview = await previewCsvFile(file);
      setPreviewA(preview);
      setMappingA(preview.suggestedMapping);
    } catch (err: any) {
      setErrorA(err.message || 'Erro ao processar prévia do arquivo ERP.');
    } finally {
      setLoadingPreviewA(false);
    }
  };

  // Handlers for File B (Acquirer)
  const handleFileBSelect = async (file: File) => {
    setFileB(file);
    setErrorB(null);
    setLoadingPreviewB(true);
    try {
      const preview = await previewCsvFile(file);
      setPreviewB(preview);
      setMappingB(preview.suggestedMapping);
    } catch (err: any) {
      setErrorB(err.message || 'Erro ao processar prévia do extrato.');
    } finally {
      setLoadingPreviewB(false);
    }
  };

  // Execute reconciliation
  const handleReconcile = async () => {
    if (!fileA || !fileB || !mappingA || !mappingB) return;

    setIsReconciling(true);
    setGeneralError(null);

    try {
      const result = await executeReconciliation(
        fileA,
        fileB,
        mappingA,
        mappingB,
        dateToleranceDays,
        amountTolerance
      );
      setReconciliationResult(result);
    } catch (err: any) {
      setGeneralError(err.message || 'Ocorreu um erro durante a conciliação.');
    } finally {
      setIsReconciling(false);
    }
  };

  // Load Brazilian Demo Data
  const handleLoadDemo = async () => {
    setIsLoadingDemo(true);
    setGeneralError(null);
    try {
      const demoResult = await fetchDemoReconciliation();
      setReconciliationResult(demoResult);
      setDateToleranceDays(demoResult.dateToleranceDays);
      setAmountTolerance(demoResult.amountTolerance);
    } catch (err: any) {
      setGeneralError(err.message || 'Falha ao carregar dados de demonstração.');
    } finally {
      setIsLoadingDemo(false);
    }
  };

  // Export to CSV
  const handleExportCsv = async () => {
    if (!reconciliationResult) return;
    setIsExporting(true);
    try {
      await downloadReconciliationCsv(reconciliationResult.items);
    } catch (err: any) {
      alert('Falha ao exportar CSV: ' + err.message);
    } finally {
      setIsExporting(false);
    }
  };

  // Reset
  const handleReset = () => {
    setFileA(null);
    setPreviewA(null);
    setMappingA(null);
    setErrorA(null);

    setFileB(null);
    setPreviewB(null);
    setMappingB(null);
    setErrorB(null);

    setReconciliationResult(null);
    setGeneralError(null);
  };

  const canReconcile = Boolean(fileA && fileB && mappingA && mappingB);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      {/* Navigation Bar */}
      <Navbar
        apiStatus={apiStatus}
        onLoadDemo={handleLoadDemo}
        isLoadingDemo={isLoadingDemo}
        onOpenHelp={() => setIsHelpOpen(true)}
      />

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full space-y-6">
        {/* Intro Banner */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">
              Conciliação Financeira de Cartões & Vendas
            </h1>
            <p className="text-xs text-slate-500 mt-1 max-w-2xl">
              Importe o relatório de vendas do seu ERP e o extrato de repasses da sua adquirente para identificar automaticamente divergências de taxas, defasagens de liquidação e vendas ausentes.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleLoadDemo}
              disabled={isLoadingDemo}
              className="inline-flex items-center px-4 py-2 text-xs font-semibold text-indigo-700 bg-indigo-50 border border-indigo-200 hover:bg-indigo-100 rounded-lg shadow-2xs transition"
            >
              <Sparkles className="h-4 w-4 mr-1.5 text-indigo-600" />
              Testar com Dados Sintéticos (Demo)
            </button>
          </div>
        </div>

        {/* Upload Areas (Side-by-side) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <UploadCard
            title="Relatório de Vendas (ERP)"
            subtitle="Exportação de vendas gerada pelo sistema de gestão (ex: Bling, Tiny, Omie, SAP)."
            badge="Fonte A"
            file={fileA}
            preview={previewA}
            mapping={mappingA}
            onFileSelect={handleFileASelect}
            onMappingChange={setMappingA}
            isLoadingPreview={loadingPreviewA}
            error={errorA}
          />

          <UploadCard
            title="Extrato da Adquirente"
            subtitle="Extrato de liquidações da maquininha ou gateway (ex: Cielo, Stone, PagBank, Rede)."
            badge="Fonte B"
            file={fileB}
            preview={previewB}
            mapping={mappingB}
            onFileSelect={handleFileBSelect}
            onMappingChange={setMappingB}
            isLoadingPreview={loadingPreviewB}
            error={errorB}
          />
        </div>

        {/* Global Error Banner */}
        {generalError && (
          <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center justify-between">
            <span>{generalError}</span>
            <button
              onClick={() => setGeneralError(null)}
              className="text-xs font-medium underline ml-4 hover:text-rose-900"
            >
              Dispensar
            </button>
          </div>
        )}

        {/* Reconciliation Controls */}
        <ReconciliationControls
          dateToleranceDays={dateToleranceDays}
          amountTolerance={amountTolerance}
          onDateToleranceChange={setDateToleranceDays}
          onAmountToleranceChange={setAmountTolerance}
          onReconcile={handleReconcile}
          onReset={handleReset}
          isProcessing={isReconciling}
          canReconcile={canReconcile}
        />

        {/* Results Section */}
        {reconciliationResult && (
          <div className="space-y-6 animate-in fade-in duration-200">
            {/* KPI Summary Cards */}
            <KpiSummary summary={reconciliationResult.summary} />

            {/* Filterable Table */}
            <FilterableResultsTable
              items={reconciliationResult.items}
              onSelectItem={(item) => setSelectedItemForModal(item)}
              onExportCsv={handleExportCsv}
              isExporting={isExporting}
            />
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-6 mt-12 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center space-x-2">
            <span className="font-semibold text-slate-700">Reconcilia AI</span>
            <span>—</span>
            <span>Auditoria e Conciliação Financeira Determinística</span>
          </div>
          <div className="flex items-center space-x-1 text-slate-400">
            <ShieldCheck className="h-3.5 w-3.5 text-slate-500" />
            <span>Processamento 100% em memória | Conformidade com privacidade de dados</span>
          </div>
        </div>
      </footer>

      {/* Modals */}
      <DiscrepancyModal
        item={selectedItemForModal}
        onClose={() => setSelectedItemForModal(null)}
      />

      <HelpModal
        isOpen={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
      />
    </div>
  );
}

export default App;
