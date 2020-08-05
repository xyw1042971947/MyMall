package com.ywxiang.mall.model;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;

public class UmsResource implements Serializable {
    private Long id;

    /**
     * 创建时间
     *
     * @mbg.generated do_not_delete_during_merge
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 资源名称
     *
     * @mbg.generated do_not_delete_during_merge
     */
    @ApiModelProperty(value = "资源名称")
    private String name;

    /**
     * 资源URL
     *
     * @mbg.generated do_not_delete_during_merge
     */
    @ApiModelProperty(value = "资源URL")
    private String url;

    /**
     * 描述
     *
     * @mbg.generated do_not_delete_during_merge
     */
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * 资源分类ID
     *
     * @mbg.generated do_not_delete_during_merge
     */
    @ApiModelProperty(value = "资源分类ID")
    private Long categoryId;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", createTime=").append(createTime);
        sb.append(", name=").append(name);
        sb.append(", url=").append(url);
        sb.append(", description=").append(description);
        sb.append(", categoryId=").append(categoryId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}