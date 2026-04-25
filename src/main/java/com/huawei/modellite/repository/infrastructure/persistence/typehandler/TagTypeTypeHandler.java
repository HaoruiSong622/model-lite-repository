package com.huawei.modellite.repository.infrastructure.persistence.typehandler;

import com.huawei.modellite.repository.common.enums.TagType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(TagType.class)
public class TagTypeTypeHandler extends BaseTypeHandler<TagType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TagType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getDbValue());
    }

    @Override
    public TagType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : TagType.fromDbValue(value);
    }

    @Override
    public TagType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : TagType.fromDbValue(value);
    }

    @Override
    public TagType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : TagType.fromDbValue(value);
    }
}