package com.spunish.common.service;

import java.util.List;

public record HistoryPage(List<HistoryEntry> entries, int page, int pageSize, boolean hasNext) {
}
