package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.village.revive.dto.NewsDTO;
import com.village.revive.dto.PageRequest;
import com.village.revive.service.NewsService;

import java.util.List;

public class NewsServiceImpl implements NewsService {
    @Override
    public IPage<NewsDTO> getNewsPage(PageRequest pageRequest, String category, String keyword, Integer status, Boolean isTop, Boolean isFeatured) {
        return null;
    }

    @Override
    public NewsDTO getNewsById(Long id) {
        return null;
    }

    @Override
    public NewsDTO createNews(NewsDTO newsDTO) {
        return null;
    }

    @Override
    public NewsDTO updateNews(Long id, NewsDTO newsDTO) {
        return null;
    }

    @Override
    public void deleteNews(Long id) {

    }

    @Override
    public List<NewsDTO> getTopNews(Integer limit) {
        return null;
    }

    @Override
    public List<NewsDTO> getFeaturedNews(Integer limit) {
        return null;
    }

    @Override
    public List<NewsDTO> getHotNews(Integer limit) {
        return null;
    }

    @Override
    public List<NewsDTO> getNewsByCategory(String category, Integer limit) {
        return null;
    }

    @Override
    public void incrementViewCount(Long id) {

    }

    @Override
    public NewsDTO publishNews(Long id) {
        return null;
    }

    @Override
    public NewsDTO unpublishNews(Long id) {
        return null;
    }

    @Override
    public Long getTotalCount() {
        return null;
    }

    @Override
    public Long getTodayCount() {
        return null;
    }

    @Override
    public Long getTotalViews() {
        return null;
    }
}
