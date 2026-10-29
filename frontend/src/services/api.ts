import { ApiStatusDto, CsvPreviewDto, ReconciliationItemDto, ReconciliationResultDto, ColumnMapping } from '../types';

const API_BASE = '/api/reconciliation';

export async function fetchApiStatus(): Promise<ApiStatusDto> {
  const res = await fetch(`${API_BASE}/status`);
  if (!res.ok) {
    throw new Error('Falha ao verificar status do servidor backend.');
  }
  return res.json();
}

export async function previewCsvFile(file: File): Promise<CsvPreviewDto> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('maxRows', '5');

  const res = await fetch(`${API_BASE}/preview`, {
    method: 'POST',
    body: formData,
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => null);
    throw new Error(errorData?.message || `Erro ao carregar prévia (${res.status})`);
  }

  return res.json();
}

export async function executeReconciliation(
  fileA: File,
  fileB: File,
  mappingA: ColumnMapping,
  mappingB: ColumnMapping,
  dateToleranceDays: number,
  amountTolerance: number
): Promise<ReconciliationResultDto> {
  const formData = new FormData();
  formData.append('fileA', fileA);
  formData.append('fileB', fileB);
  formData.append('mappingAJson', JSON.stringify(mappingA));
  formData.append('mappingBJson', JSON.stringify(mappingB));
  formData.append('dateToleranceDays', dateToleranceDays.toString());
  formData.append('amountTolerance', amountTolerance.toFixed(2));

  const res = await fetch(`${API_BASE}/process`, {
    method: 'POST',
    body: formData,
  });

  if (!res.ok) {
    const errorData = await res.json().catch(() => null);
    throw new Error(errorData?.message || `Erro no processamento da conciliação (${res.status})`);
  }

  return res.json();
}

export async function fetchDemoReconciliation(): Promise<ReconciliationResultDto> {
  const res = await fetch(`${API_BASE}/demo`);
  if (!res.ok) {
    throw new Error('Falha ao carregar dados sintéticos de demonstração.');
  }
  return res.json();
}

export async function downloadReconciliationCsv(items: ReconciliationItemDto[]): Promise<void> {
  const res = await fetch(`${API_BASE}/export`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(items),
  });

  if (!res.ok) {
    throw new Error('Falha ao exportar conciliação para CSV.');
  }

  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `conciliacao_reconcilia_ai_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
