package com.tobacco.weight.data;

import java.util.Date;

/**
 * 地区信息数据模型
 * 用于存储乡镇和村庄的层级信息
 */
public class LocationInfo {
    
    private Long id; // 数据库主键ID
    private String townshipName; // 乡镇名称
    private String villageName; // 村庄名称
    private String locationType; // 地区类型：township(乡镇) 或 village(村庄)
    private Long parentId; // 父级ID（村庄指向乡镇）
    private int displayOrder; // 显示排序
    private boolean isActive; // 是否启用
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间

    public LocationInfo() {
        this.isActive = true;
        this.displayOrder = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * 创建乡镇信息
     */
    public static LocationInfo createTownship(String townshipName, int displayOrder) {
        LocationInfo location = new LocationInfo();
        location.setTownshipName(townshipName);
        location.setVillageName(""); // 乡镇级别村庄名为空
        location.setLocationType("township");
        location.setDisplayOrder(displayOrder);
        return location;
    }

    /**
     * 创建村庄信息
     */
    public static LocationInfo createVillage(String townshipName, String villageName, Long parentId, int displayOrder) {
        LocationInfo location = new LocationInfo();
        location.setTownshipName(townshipName);
        location.setVillageName(villageName);
        location.setLocationType("village");
        location.setParentId(parentId);
        location.setDisplayOrder(displayOrder);
        return location;
    }

    /**
     * 是否为乡镇级别
     */
    public boolean isTownship() {
        return "township".equals(locationType);
    }

    /**
     * 是否为村庄级别
     */
    public boolean isVillage() {
        return "village".equals(locationType);
    }

    /**
     * 获取完整地址（乡镇 + 村庄）
     */
    public String getFullAddress() {
        if (isTownship()) {
            return townshipName;
        } else {
            return townshipName + " " + villageName;
        }
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return isVillage() ? villageName : townshipName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTownshipName() {
        return townshipName;
    }

    public void setTownshipName(String townshipName) {
        this.townshipName = townshipName;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "LocationInfo{" +
                "id=" + id +
                ", townshipName='" + townshipName + '\'' +
                ", villageName='" + villageName + '\'' +
                ", locationType='" + locationType + '\'' +
                ", parentId=" + parentId +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationInfo)) return false;

        LocationInfo that = (LocationInfo) o;

        if (!townshipName.equals(that.townshipName)) return false;
        return villageName.equals(that.villageName);
    }

    @Override
    public int hashCode() {
        int result = townshipName.hashCode();
        result = 31 * result + villageName.hashCode();
        return result;
    }
}