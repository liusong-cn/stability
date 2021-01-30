
package com.bzzn.stability.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * page info
 *
 * @param <T> model
 * @author unascribed
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
public class PageInfo<T> {

    /**
     * list
     */
    @ApiModelProperty(value = "分页数据列表", required = true, dataType = "List")
    protected List<T> list;
    /**
     * total count
     */
    @ApiModelProperty(value = "总记录数", dataType = "Integer")
    protected Integer totalCount = 0;
    /**
     * page size
     */
    @ApiModelProperty(value = "页面记录数", dataType = "Integer")
    protected Integer pageSize = 20;
    /**
     * current page
     */
    @ApiModelProperty(value = "当前页数", dataType = "Integer")
    protected Integer currPage = 0;
    /**
     * pageNo
     */
    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private Integer pageNo;

    public PageInfo(Integer currPage, Integer pageSize) {
        if (currPage == null) {
            currPage = 1;
        }
        this.pageNo = (currPage - 1) * pageSize;
        this.pageSize = pageSize;
        this.currPage = currPage;
    }

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    public Integer getStart() {
        return pageNo;
    }

    public void setStart(Integer start) {
        this.pageNo = start;
    }

    @ApiModelProperty("总页数")
    public Integer getTotalPage() {
        if (pageSize == null || pageSize == 0) {
            pageSize = 7;
        }
        if (this.totalCount % this.pageSize == 0) {
            return (this.totalCount / this.pageSize) == 0 ? 1 : (this.totalCount / this.pageSize);
        }
        return (this.totalCount / this.pageSize + 1);
    }

    /**
     * 将Mybatis Plus的IPage 转换为PageInfo
     *
     * @param rawPageInfo 分页信息
     * @param <T>         T
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(IPage<T> rawPageInfo) {
        PageInfo<T> pageInfo = new PageInfo<>();
        pageInfo.setTotalCount((int) rawPageInfo.getTotal());
        pageInfo.setCurrPage((int) rawPageInfo.getCurrent());
        pageInfo.setPageSize((int) rawPageInfo.getSize());
        pageInfo.setList(rawPageInfo.getRecords());
        return pageInfo;
    }

    /**
     * 将Page Helper 的pageInfo 转换为PageInfo
     *
     * @param rawPageInfo 分页信息
     * @param <T>         T
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(com.github.pagehelper.PageInfo<T> rawPageInfo) {
        PageInfo<T> pageInfo = new PageInfo<>();
        pageInfo.setTotalCount((int) rawPageInfo.getTotal());
        pageInfo.setCurrPage(rawPageInfo.getPageNum());
        pageInfo.setPageSize(rawPageInfo.getPageSize());
        pageInfo.setList(rawPageInfo.getList());
        return pageInfo;
    }

    public interface BeanProcessor<T, E> {
        /**
         * bean 特殊值转换
         *
         * @param raw    原始对象
         * @param target 目标对象
         */
        void process(T raw, E target);
    }


}