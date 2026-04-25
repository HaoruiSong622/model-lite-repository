package com.huawei.modellite.repository.infrastructure.persistence.typehandler;

import com.huawei.modellite.repository.common.enums.SourceType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(SourceType.class)
public class SourceTypeTypeHandler extends BaseTypeHandler<SourceType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, SourceType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getDbValue());
    }

    @Override
    public SourceType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : SourceType.fromDbValue(value);
    }

    @Override
    public SourceType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : SourceType.fromDbValue(value);
    }

    @Override
    public SourceType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : SourceType.fromDbValue(value);
    }
}