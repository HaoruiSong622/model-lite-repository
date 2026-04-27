package com.huawei.modellite.repository.infrastructure.persistence.typehandler;

import com.huawei.modellite.repository.modelweight.domain.aggregate.model.SourcePathType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(SourcePathType.class)
public class SourcePathTypeTypeHandler extends BaseTypeHandler<SourcePathType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, SourcePathType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getDbValue());
    }

    @Override
    public SourcePathType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : SourcePathType.fromDbValue(value);
    }

    @Override
    public SourcePathType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : SourcePathType.fromDbValue(value);
    }

    @Override
    public SourcePathType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : SourcePathType.fromDbValue(value);
    }
}
