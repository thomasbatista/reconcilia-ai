export type ReconciliationStatus = 'MATCHED' | 'AMOUNT_MISMATCH' | 'DATE_MISMATCH' | 'UNMATCHED';

export interface NormalizedTransaction {
  id: string;
  date: string;
  amount: number;
  description: string;
  source: string;
  rowNumber: number;
}

export interface ReconciliationItemDto {
  id: string;
  status: ReconciliationStatus;
  statusLabel: string;
  sourceA: NormalizedTransaction | null;
  sourceB: NormalizedTransaction | null;
  amountDifference: number | null;
  dateDifferenceDays: number | null;
  matchReason: string;
  aiHypothesis: string | null;
  isHypothesisAi: boolean;
}

export interface ReconciliationSummary {
  totalSourceACount: number;
  totalSourceAAmount: number;
  totalSourceBCount: number;
  totalSourceBAmount: number;
  matchedCount: number;
  matchedAmountSourceA: number;
  matchedAmountSourceB: number;
  amountMismatchCount: number;
  dateMismatchCount: number;
  unmatchedSourceACount: number;
  unmatchedSourceAAmount: number;
  unmatchedSourceBCount: number;
  unmatchedSourceBAmount: number;
  netDiscrepancyAmount: number;
  matchRatePercentage: number;
}

export interface ReconciliationResultDto {
  summary: ReconciliationSummary;
  items: ReconciliationItemDto[];
  dateToleranceDays: number;
  amountTolerance: number;
}

export interface ColumnMapping {
  idColumn: string;
  dateColumn: string;
  amountColumn: string;
  descriptionColumn: string;
}

export interface CsvPreviewDto {
  filename: string;
  totalRows: number;
  headers: string[];
  suggestedMapping: ColumnMapping;
  previewRows: Record<string, string>[];
}

export interface ApiStatusDto {
  status: string;
  appName: string;
  version: string;
  aiAvailable: boolean;
  defaultDateToleranceDays: number;
  defaultAmountTolerance: string;
}
